package com.dearlordylord.quint.idea.annotator

import com.dearlordylord.quint.idea.settings.QuintSettingsState
import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.ExternalAnnotator
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.Document
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiFile
import java.util.concurrent.ConcurrentHashMap
import java.io.File
import java.nio.charset.StandardCharsets

data class QuintAnnotatorInput(
    val filePath: String,
    val documentText: String,
    val modificationStamp: Long,
    val cachedResult: QuintTypecheckResult?,
    val toolRunner: QuintToolRunner
)

data class QuintAnnotationResult(
    val modificationStamp: Long,
    val typecheckResult: QuintTypecheckResult
)

class QuintExternalAnnotator : ExternalAnnotator<QuintAnnotatorInput, QuintAnnotationResult>() {
    companion object {
        private val LOG = Logger.getInstance(QuintExternalAnnotator::class.java)
        private val resultCache = ConcurrentHashMap<String, CachedTypecheckResult>()

        // Visible for testing
        @Volatile var toolRunnerFactory: (() -> QuintToolRunner)? = null

        internal fun clearCacheForTests() {
            resultCache.clear()
        }
    }

    override fun collectInformation(file: PsiFile): QuintAnnotatorInput? {
        val binaryPath = QuintSettingsState.getInstance().resolveQuintPath()
        if (binaryPath == null) return null

        val virtualFile = file.virtualFile ?: return null
        val document = PsiDocumentManager.getInstance(file.project).getDocument(file) ?: return null
        val scheduler = QuintTypecheckSchedulingService.getInstance()
        if (scheduler.shouldDefer(document)) return null

        val modificationStamp = document.modificationStamp
        val cachedResult = resultCache[virtualFile.path]?.takeIf { it.modificationStamp == modificationStamp }?.result

        val toolRunner = toolRunnerFactory?.invoke() ?: QuintCliToolRunner()
        return QuintAnnotatorInput(
            filePath = virtualFile.path,
            documentText = document.text,
            modificationStamp = modificationStamp,
            cachedResult = cachedResult,
            toolRunner = toolRunner
        )
    }

    override fun doAnnotate(collectedInfo: QuintAnnotatorInput?): QuintAnnotationResult? {
        if (collectedInfo == null) return null
        collectedInfo.cachedResult?.let {
            return QuintAnnotationResult(collectedInfo.modificationStamp, it)
        }

        val originalFile = File(collectedInfo.filePath)
        val parentDir = originalFile.parentFile ?: return null

        // Temp file in same dir so quint resolves relative imports
        var tempFile: File? = null
        return try {
            tempFile = File.createTempFile(".quint-idea-", "-${originalFile.name}", parentDir)
            tempFile.writeText(collectedInfo.documentText, StandardCharsets.UTF_8)
            val result = collectedInfo.toolRunner.typecheck(tempFile.canonicalPath)
            val annotationResult = QuintAnnotationResult(
                modificationStamp = collectedInfo.modificationStamp,
                typecheckResult = remapSource(result, tempFile.canonicalPath, collectedInfo.filePath)
            )
            resultCache[collectedInfo.filePath] = CachedTypecheckResult(
                collectedInfo.modificationStamp,
                annotationResult.typecheckResult
            )
            annotationResult
        } catch (e: Exception) {
            LOG.warn("Quint typecheck failed: ${e.message}")
            null
        } finally {
            tempFile?.delete()
        }
    }

    override fun apply(file: PsiFile, annotationResult: QuintAnnotationResult?, holder: AnnotationHolder) {
        if (annotationResult == null) return

        val document = PsiDocumentManager.getInstance(file.project).getDocument(file) ?: return
        val filePath = file.virtualFile?.path ?: return
        if (document.modificationStamp != annotationResult.modificationStamp) return

        resultCache[filePath] = CachedTypecheckResult(document.modificationStamp, annotationResult.typecheckResult)

        for (error in annotationResult.typecheckResult.errors) {
            applyAnnotation(error, filePath, document, holder, HighlightSeverity.ERROR)
        }
        for (warning in annotationResult.typecheckResult.warnings) {
            applyAnnotation(warning, filePath, document, holder, HighlightSeverity.WARNING)
        }

        // Cache type data for the documentation provider
        val virtualFile = file.virtualFile
        if (virtualFile != null && annotationResult.typecheckResult.modules.isNotEmpty()) {
            QuintTypeCache.update(virtualFile, annotationResult.typecheckResult)
        }
    }

    private fun applyAnnotation(
        error: QuintError,
        filePath: String,
        document: Document,
        holder: AnnotationHolder,
        severity: HighlightSeverity
    ) {
        val message = error.explanation.trim()
        if (message.isEmpty()) return

        for (loc in error.locs) {
            // Only annotate locations in this file
            if (loc.source != filePath) continue

            val textRange = computeTextRange(loc, document) ?: continue
            holder.newAnnotation(severity, message.substringBefore('\n'))
                .range(textRange)
                .tooltip(message)
                .create()
        }

        // If no locs matched this file, but error has no locs at all, annotate start of file
        if (error.locs.isEmpty()) {
            holder.newAnnotation(severity, message.substringBefore('\n'))
                .range(TextRange(0, minOf(1, document.textLength)))
                .tooltip(message)
                .create()
        }
    }

    private fun computeTextRange(loc: QuintErrorLocation, document: Document): TextRange? {
        val startLine = loc.start.line
        val endLine = loc.end.line
        val startCol = loc.start.col
        val endCol = loc.end.col

        // Quint uses 0-based lines, end col is inclusive
        if (startLine < 0 || startLine >= document.lineCount) return null
        if (endLine < 0 || endLine >= document.lineCount) return null

        val startOffset = document.getLineStartOffset(startLine) + startCol
        val endOffset = document.getLineStartOffset(endLine) + endCol

        // Ensure at least 1 char is highlighted
        val adjustedEnd = if (endOffset <= startOffset) startOffset + 1 else endOffset + 1

        val safeStart = startOffset.coerceIn(0, document.textLength)
        val safeEnd = adjustedEnd.coerceIn(safeStart, document.textLength)

        if (safeStart == safeEnd) return null

        return TextRange(safeStart, safeEnd)
    }
}

private data class CachedTypecheckResult(
    val modificationStamp: Long,
    val result: QuintTypecheckResult
)

private fun remapSource(result: QuintTypecheckResult, from: String, to: String): QuintTypecheckResult {
    fun List<QuintError>.remap() = map { error ->
        error.copy(locs = error.locs.map { loc ->
            if (loc.source == from) loc.copy(source = to) else loc
        })
    }
    return result.copy(
        errors = result.errors.remap(),
        warnings = result.warnings.remap()
    )
}
