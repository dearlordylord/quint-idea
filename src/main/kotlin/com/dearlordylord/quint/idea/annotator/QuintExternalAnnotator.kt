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
import com.intellij.psi.PsiFile
import java.io.File
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.concurrent.ConcurrentHashMap

data class QuintAnnotatorInput(
    val filePath: String,
    val documentText: String,
    val contentHash: Int,
    val cachedResult: QuintTypecheckResult?,
    val skipTypecheck: Boolean,
    val toolRunner: QuintToolRunner
)

data class QuintAnnotationResult(
    val contentHash: Int,
    val typecheckResult: QuintTypecheckResult
)

class QuintExternalAnnotator : ExternalAnnotator<QuintAnnotatorInput, QuintAnnotationResult>() {
    companion object {
        private val LOG = Logger.getInstance(QuintExternalAnnotator::class.java)
        private val resultCache = ConcurrentHashMap<String, CachedTypecheckResult>()

        @Volatile var toolRunnerFactory: (() -> QuintToolRunner)? = null

        internal fun clearCacheForTests() {
            resultCache.clear()
        }
    }

    override fun collectInformation(file: PsiFile, editor: Editor, hasErrors: Boolean): QuintAnnotatorInput? =
        collectInformation(file)

    override fun collectInformation(file: PsiFile): QuintAnnotatorInput? {
        if (QuintSettingsState.getInstance().resolveQuintPath() == null) return null

        // Daemon often hands us a non-physical "highlighting copy" whose document is
        // a fresh in-memory snapshot with modStamp=0. Always use the editor's real
        // document via the original file's VirtualFile.
        val virtualFile = file.originalFile.virtualFile ?: file.virtualFile ?: return null
        val document = FileDocumentManager.getInstance().getDocument(virtualFile) ?: return null

        val path = virtualFile.path
        val documentText = document.charsSequence.toString()
        val contentHash = documentText.hashCode()
        val cached = resultCache[path]
        val cacheHit = cached?.contentHash == contentHash
        val deferring = QuintTypecheckSchedulingService.getInstance().shouldDefer(document)
        val skipTypecheck = deferring || cacheHit

        return QuintAnnotatorInput(
            filePath = path,
            documentText = documentText,
            contentHash = contentHash,
            cachedResult = if (cacheHit) cached?.result else null,
            skipTypecheck = skipTypecheck,
            toolRunner = if (skipTypecheck) DummyToolRunner else toolRunnerFactory?.invoke() ?: QuintCliToolRunner()
        )
    }

    override fun doAnnotate(collectedInfo: QuintAnnotatorInput?): QuintAnnotationResult? {
        if (collectedInfo == null) return null
        val path = collectedInfo.filePath

        if (collectedInfo.skipTypecheck) {
            return collectedInfo.cachedResult?.let {
                QuintAnnotationResult(collectedInfo.contentHash, it)
            }
        }

        val originalFile = File(path)
        val parentDir = originalFile.parentFile ?: return null

        return try {
            val workspaceRoot = mirrorWorkspace(parentDir, originalFile.name, collectedInfo.documentText)
            val pathForQuint = File(workspaceRoot, originalFile.name).canonicalPath
            val raw = collectedInfo.toolRunner.typecheck(pathForQuint)
            val result = remapAllSources(raw, workspaceRoot.canonicalPath, parentDir.canonicalPath)
            resultCache[path] = CachedTypecheckResult(collectedInfo.contentHash, result)
            QuintAnnotationResult(collectedInfo.contentHash, result)
        } catch (e: Exception) {
            LOG.warn("Quint typecheck failed for $path: ${e.message}", e)
            null
        }
    }

    /**
     * Hard-links (or copies) sibling .qnt files from [sourceDir] into a stable mirror
     * under the system tmp dir so relative imports resolve, then writes [targetText] as
     * the snapshot for [targetName]. Keeps nothing in the user's source dir.
     */
    private fun mirrorWorkspace(sourceDir: File, targetName: String, targetText: String): File {
        val workspace = File(
            System.getProperty("java.io.tmpdir"),
            "quint-idea-${Integer.toHexString(sourceDir.canonicalPath.hashCode())}"
        )
        workspace.mkdirs()

        sourceDir.listFiles { f -> f.isFile && f.extension == "qnt" && f.name != targetName }?.forEach { sibling ->
            val mirror = File(workspace, sibling.name)
            if (mirror.exists() && mirror.lastModified() >= sibling.lastModified()) return@forEach
            mirror.delete()
            try {
                Files.createLink(mirror.toPath(), sibling.toPath())
            } catch (_: Exception) {
                Files.copy(sibling.toPath(), mirror.toPath(), StandardCopyOption.REPLACE_EXISTING)
            }
        }

        val target = File(workspace, targetName)
        target.delete()
        target.writeText(targetText, StandardCharsets.UTF_8)
        return workspace
    }

    override fun apply(file: PsiFile, annotationResult: QuintAnnotationResult?, holder: AnnotationHolder) {
        if (annotationResult == null) return

        val virtualFile = file.originalFile.virtualFile ?: file.virtualFile ?: return
        val document = FileDocumentManager.getInstance().getDocument(virtualFile) ?: return

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

/** No-op runner used when we'll skip typecheck anyway — avoids allocating a real one on cache hits. */
private object DummyToolRunner : QuintToolRunner {
    override fun typecheck(filePath: String): QuintTypecheckResult =
        QuintTypecheckResult(stage = "skipped", errors = emptyList(), warnings = emptyList())
}

/** Remap source paths from a mirror dir back to the user's original directory. */
private fun remapAllSources(result: QuintTypecheckResult, fromDir: String, toDir: String): QuintTypecheckResult {
    val fromPrefix = if (fromDir.endsWith(File.separator)) fromDir else fromDir + File.separator
    val toPrefix = if (toDir.endsWith(File.separator)) toDir else toDir + File.separator
    fun remapPath(p: String): String =
        if (p.startsWith(fromPrefix)) toPrefix + p.substring(fromPrefix.length) else p
    fun List<QuintError>.remap() = map { error ->
        error.copy(locs = error.locs.map { loc -> loc.copy(source = remapPath(loc.source)) })
    }
    return result.copy(errors = result.errors.remap(), warnings = result.warnings.remap())
}
