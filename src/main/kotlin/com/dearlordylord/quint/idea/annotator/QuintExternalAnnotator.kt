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
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.progress.ProcessCanceledException

data class QuintAnnotatorInput(
    val filePath: String,
    val documentText: String,
    val contentHash: Int,
    val cachedResult: QuintTypecheckResult?,
    val skipTypecheck: Boolean,
    val toolRunner: QuintToolRunner?,
    val snapshot: QuintAnalysisSnapshot,
    val checking: QuintCheckingService,
    val generation: Long
)

data class QuintAnnotationResult(val typecheckResult: QuintTypecheckResult, val snapshot: QuintAnalysisSnapshot? = null, val checking: QuintCheckingService? = null, val generation: Long = 0)

class QuintExternalAnnotator : ExternalAnnotator<QuintAnnotatorInput, QuintAnnotationResult>() {
    companion object {
        private val LOG = Logger.getInstance(QuintExternalAnnotator::class.java)

        @TestOnly @Volatile var toolRunnerFactory: (() -> QuintToolRunner)? = null

        @TestOnly
        internal fun clearCacheForTests() {
            ProjectManager.getInstance().openProjects.forEach { QuintCheckingService.getInstance(it).clear() }
        }
    }

    override fun collectInformation(file: PsiFile, editor: Editor, hasErrors: Boolean): QuintAnnotatorInput? =
        collectInformation(file)

    override fun collectInformation(file: PsiFile): QuintAnnotatorInput? = collect(file, false)

    fun collectForManualCheck(file: PsiFile): QuintAnnotatorInput? = collect(file, true)

    private fun collect(file: PsiFile, manual: Boolean): QuintAnnotatorInput? {

        val (virtualFile, document) = resolveEditorDocument(file) ?: return null
        val path = virtualFile.path
        val documentText = document.charsSequence.toString()
        val contentHash = documentText.hashCode()
        val checking = QuintCheckingService.getInstance(file.project)
        val snapshot = QuintAnalysisSnapshot.capture(path, documentText, QuintSettingsState.getInstance().resolveQuintPath(), virtualFile.fileSystem.protocol)
        val cached = checking.cached(virtualFile, snapshot)
        val cacheHit = cached != null
        val skipTypecheck = !manual && (cacheHit || !QuintSettingsState.getInstance().backgroundChecking || QuintTypecheckSchedulingService.getInstance().shouldDefer(document))

        return QuintAnnotatorInput(
            filePath = path,
            documentText = documentText,
            contentHash = contentHash,
            cachedResult = cached,
            skipTypecheck = skipTypecheck,
            toolRunner = if (skipTypecheck) null else (toolRunnerFactory?.invoke() ?: QuintCliToolRunner(snapshot.executable, file.project)),
            snapshot = snapshot,
            checking = checking,
            generation = if (skipTypecheck) checking.state(virtualFile)?.generation ?: 0 else checking.begin(virtualFile, snapshot)
        )
    }

    override fun doAnnotate(collectedInfo: QuintAnnotatorInput?): QuintAnnotationResult? {
        if (collectedInfo == null) return null
        val path = collectedInfo.filePath

        if (collectedInfo.skipTypecheck) {
            return collectedInfo.cachedResult?.let { QuintAnnotationResult(it, collectedInfo.snapshot, collectedInfo.checking, collectedInfo.generation) }
        }

        val runner = collectedInfo.toolRunner ?: return null

        return try {
            val result = QuintTypecheckExecutor(runner).typecheck(collectedInfo.snapshot)
            if (!collectedInfo.checking.finish(path, collectedInfo.generation, result)) return null
            QuintAnnotationResult(result, collectedInfo.snapshot, collectedInfo.checking, collectedInfo.generation)
        } catch (e: ProcessCanceledException) {
            collectedInfo.checking.cancel(path, collectedInfo.generation)
            throw e
        } catch (e: QuintToolFailure) {
            collectedInfo.checking.finish(path, collectedInfo.generation, null, e)
            null
        } catch (e: Exception) {
            collectedInfo.checking.finish(path, collectedInfo.generation, null, QuintToolFailure(QuintCheckingStatus.FAILED, e.message ?: "Check failed"))
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

        if (annotationResult.snapshot?.isCurrent(document.text) == false) return
        if (annotationResult.checking?.accepts(virtualFile, annotationResult.generation, annotationResult.snapshot!!) == false) return
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
        if (result.errors.isEmpty() && result.modules.isNotEmpty() && result.types.isNotEmpty()) {
            QuintTypeCache.update(virtualFile, result, annotationResult.snapshot)
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

        // Quint columns count Unicode code points; IntelliJ ranges use UTF-16 offsets.
        fun offset(line: Int, column: Int, inclusiveEnd: Boolean): Int? {
            val lineStart = document.getLineStartOffset(line)
            val text = document.charsSequence.subSequence(lineStart, document.getLineEndOffset(line)).toString()
            val count = text.codePointCount(0, text.length)
            if (column < 0 || column > count) return null
            val points = if (inclusiveEnd) minOf(column + 1, count) else column
            return lineStart + text.offsetByCodePoints(0, points)
        }
        val safeStart = offset(startLine, loc.start.col, false) ?: return null
        val safeEnd = offset(endLine, loc.end.col, true) ?: return null
        if (safeEnd < safeStart) return null
        return if (safeStart == safeEnd) null else TextRange(safeStart, safeEnd)
    }
}
