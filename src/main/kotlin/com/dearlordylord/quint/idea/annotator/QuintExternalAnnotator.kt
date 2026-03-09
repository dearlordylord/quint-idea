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

data class QuintAnnotatorInput(
    val filePath: String,
    val toolRunner: QuintToolRunner
)

class QuintExternalAnnotator : ExternalAnnotator<QuintAnnotatorInput, QuintTypecheckResult>() {
    companion object {
        private val LOG = Logger.getInstance(QuintExternalAnnotator::class.java)

        // Visible for testing
        var toolRunnerFactory: (() -> QuintToolRunner)? = null
    }

    override fun collectInformation(file: PsiFile): QuintAnnotatorInput? {
        val binaryPath = QuintSettingsState.getInstance().quintBinaryPath
        if (binaryPath.isBlank()) {
            return null
        }

        val virtualFile = file.virtualFile ?: return null
        val filePath = virtualFile.path

        val toolRunner = toolRunnerFactory?.invoke() ?: QuintCliToolRunner()
        return QuintAnnotatorInput(filePath, toolRunner)
    }

    override fun doAnnotate(collectedInfo: QuintAnnotatorInput?): QuintTypecheckResult? {
        if (collectedInfo == null) return null
        return try {
            collectedInfo.toolRunner.typecheck(collectedInfo.filePath)
        } catch (e: Exception) {
            LOG.warn("Quint typecheck failed: ${e.message}")
            null
        }
    }

    override fun apply(file: PsiFile, annotationResult: QuintTypecheckResult?, holder: AnnotationHolder) {
        if (annotationResult == null) return

        val document = PsiDocumentManager.getInstance(file.project).getDocument(file) ?: return
        val filePath = file.virtualFile?.path ?: return

        for (error in annotationResult.errors) {
            applyAnnotation(error, filePath, document, holder, HighlightSeverity.ERROR)
        }
        for (warning in annotationResult.warnings) {
            applyAnnotation(warning, filePath, document, holder, HighlightSeverity.WARNING)
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
            holder.newAnnotation(severity, message.lines().first())
                .range(textRange)
                .tooltip(message)
                .create()
        }

        // If no locs matched this file, but error has no locs at all, annotate start of file
        if (error.locs.isEmpty()) {
            holder.newAnnotation(severity, message.lines().first())
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

        // Quint uses 0-based lines
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
