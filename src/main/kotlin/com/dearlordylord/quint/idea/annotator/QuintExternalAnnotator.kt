package com.dearlordylord.quint.idea.annotator

import com.dearlordylord.quint.idea.settings.QuintSettingsState
import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.ExternalAnnotator
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.util.TextRange
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiFile
import org.jetbrains.annotations.TestOnly
import java.util.concurrent.ConcurrentHashMap

data class QuintAnnotatorInput(
    val filePath: String,
    val documentText: String,
    val contentHash: Int,
    val cachedResult: QuintTypecheckResult?,
    val skipTypecheck: Boolean,
    val toolRunner: QuintToolRunner?
)

data class QuintAnnotationResult(val typecheckResult: QuintTypecheckResult)

class QuintExternalAnnotator : ExternalAnnotator<QuintAnnotatorInput, QuintAnnotationResult>() {
    companion object {
        private val LOG = Logger.getInstance(QuintExternalAnnotator::class.java)
        private val resultCache = ConcurrentHashMap<String, CachedTypecheckResult>()

        @TestOnly @Volatile var toolRunnerFactory: (() -> QuintToolRunner)? = null

        @TestOnly
        internal fun clearCacheForTests() {
            resultCache.clear()
        }
    }

    override fun collectInformation(file: PsiFile, editor: Editor, hasErrors: Boolean): QuintAnnotatorInput? =
        collectInformation(file)

    override fun collectInformation(file: PsiFile): QuintAnnotatorInput? {
        if (QuintSettingsState.getInstance().resolveQuintPath() == null) return null

        val (virtualFile, document) = resolveEditorDocument(file) ?: return null
        val path = virtualFile.path
        val documentText = document.charsSequence.toString()
        val contentHash = documentText.hashCode()
        val cached = resultCache[path]
        val cacheHit = cached?.contentHash == contentHash
        val skipTypecheck = cacheHit || QuintTypecheckSchedulingService.getInstance().shouldDefer(document)

        return QuintAnnotatorInput(
            filePath = path,
            documentText = documentText,
            contentHash = contentHash,
            cachedResult = if (cacheHit) cached?.result else null,
            skipTypecheck = skipTypecheck,
            toolRunner = if (skipTypecheck) null else (toolRunnerFactory?.invoke() ?: QuintCliToolRunner())
        )
    }

    override fun doAnnotate(collectedInfo: QuintAnnotatorInput?): QuintAnnotationResult? {
        if (collectedInfo == null) return null
        val path = collectedInfo.filePath

        if (collectedInfo.skipTypecheck) {
            return collectedInfo.cachedResult?.let { QuintAnnotationResult(it) }
        }

        val runner = collectedInfo.toolRunner ?: return null

        return try {
            val snapshot = QuintFileSnapshot(path, collectedInfo.documentText)
            val result = QuintTypecheckExecutor(runner).typecheck(snapshot) ?: return null
            resultCache[path] = CachedTypecheckResult(collectedInfo.contentHash, result)
            QuintAnnotationResult(result)
        } catch (e: Exception) {
            LOG.warn("Quint typecheck failed for $path: ${e.message}", e)
            null
        }
    }

    /**
     * Resolve the editor's real VirtualFile + Document. The daemon sometimes hands us a
     * non-physical "highlighting copy" whose own document is a fresh snapshot with
     * modStamp=0 — useless for caching. `originalFile.virtualFile` points at the real one.
     */
    private fun resolveEditorDocument(file: PsiFile): Pair<VirtualFile, Document>? {
        val virtualFile = file.originalFile.virtualFile ?: file.virtualFile ?: return null
        val document = FileDocumentManager.getInstance().getDocument(virtualFile) ?: return null
        return virtualFile to document
    }

    override fun apply(file: PsiFile, annotationResult: QuintAnnotationResult?, holder: AnnotationHolder) {
        if (annotationResult == null) return
        val (virtualFile, document) = resolveEditorDocument(file) ?: return

        val result = annotationResult.typecheckResult
        for (error in result.errors) {
            applyAnnotation(error, virtualFile.path, document, holder, HighlightSeverity.ERROR)
        }
        for (warning in result.warnings) {
            applyAnnotation(warning, virtualFile.path, document, holder, HighlightSeverity.WARNING)
        }

        // When typecheck has errors quint returns modules with an empty types map;
        // replacing our cache with it would wipe the last good type data and make
        // hover go blank. Preserve the previous types instead.
        if (result.modules.isNotEmpty() && result.types.isNotEmpty()) {
            QuintTypeCache.update(virtualFile, result)
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

        var matched = false
        for (loc in error.locs) {
            if (loc.source != filePath) continue
            val textRange = computeTextRange(loc, document) ?: continue
            holder.newAnnotation(severity, message.substringBefore('\n'))
                .range(textRange)
                .tooltip(message)
                .create()
            matched = true
        }

        if (!matched && error.locs.isEmpty()) {
            holder.newAnnotation(severity, message.substringBefore('\n'))
                .range(TextRange(0, minOf(1, document.textLength)))
                .tooltip(message)
                .create()
        }
    }

    private fun computeTextRange(loc: QuintErrorLocation, document: Document): TextRange? {
        val startLine = loc.start.line
        val endLine = loc.end.line
        if (startLine < 0 || startLine >= document.lineCount) return null
        if (endLine < 0 || endLine >= document.lineCount) return null

        val startOffset = document.getLineStartOffset(startLine) + loc.start.col
        val endOffset = document.getLineStartOffset(endLine) + loc.end.col
        // Quint's end column is inclusive; PSI ranges are exclusive.
        val adjustedEnd = if (endOffset <= startOffset) startOffset + 1 else endOffset + 1

        val safeStart = startOffset.coerceIn(0, document.textLength)
        val safeEnd = adjustedEnd.coerceIn(safeStart, document.textLength)
        return if (safeStart == safeEnd) null else TextRange(safeStart, safeEnd)
    }
}

private data class CachedTypecheckResult(
    val contentHash: Int,
    val result: QuintTypecheckResult
)
