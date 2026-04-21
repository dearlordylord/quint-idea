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
    /** The exact text snapshot we will hand to quint; cache key matches this. */
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

        // Visible for testing
        @Volatile var toolRunnerFactory: (() -> QuintToolRunner)? = null

        internal fun clearCacheForTests() {
            resultCache.clear()
        }
    }

    override fun collectInformation(file: PsiFile, editor: Editor, hasErrors: Boolean): QuintAnnotatorInput? =
        collectInformation(file)

    override fun collectInformation(file: PsiFile): QuintAnnotatorInput? {
        val binaryPath = QuintSettingsState.getInstance().resolveQuintPath()
        if (binaryPath == null) return null

        // The daemon often hands us a non-physical "highlighting copy" of the PsiFile.
        // Resolve to the original file's VirtualFile so we always look at the real document.
        val originalPsi = file.originalFile
        val virtualFile = originalPsi.virtualFile ?: file.virtualFile ?: return null
        val document = FileDocumentManager.getInstance().getDocument(virtualFile) ?: return null

        val path = virtualFile.path
        // Snapshot the text NOW. This is what quint will typecheck and what the
        // cache key will reference. Using save-and-read-disk has a race: between save
        // and quint reading the file, the user may have typed more, so the cache key
        // would not match what quint actually saw. Snapshotting eliminates the race.
        val documentText = document.charsSequence.toString()
        val contentHash = documentText.hashCode()
        val cached = resultCache[path]
        val cacheHit = cached?.contentHash == contentHash
        val cacheToEcho = if (cacheHit) cached?.result else null
        val deferring = QuintTypecheckSchedulingService.getInstance().shouldDefer(document)

        LOG.info("collectInformation $path: hash=$contentHash deferring=$deferring cacheHit=$cacheHit (psiCopy=${originalPsi !== file})")
        return QuintAnnotatorInput(
            filePath = path,
            documentText = documentText,
            contentHash = contentHash,
            cachedResult = cacheToEcho,
            skipTypecheck = deferring || cacheHit,
            toolRunner = toolRunnerFactory?.invoke() ?: QuintCliToolRunner()
        )
    }

    override fun doAnnotate(collectedInfo: QuintAnnotatorInput?): QuintAnnotationResult? {
        if (collectedInfo == null) return null
        val path = collectedInfo.filePath

        if (collectedInfo.skipTypecheck) {
            val echo = collectedInfo.cachedResult
            LOG.info("doAnnotate $path: skip typecheck (cached=${echo != null}, errors=${echo?.errors?.size ?: 0})")
            return echo?.let { QuintAnnotationResult(collectedInfo.contentHash, it) }
        }

        // Snapshot the document into a private workspace under the system tmp dir,
        // copy the sibling .qnt files alongside so quint can resolve relative imports,
        // and run quint there. Nothing is written into the user's repo.
        val originalFile = File(path)
        val parentDir = originalFile.parentFile?.takeIf { it.exists() && it.isDirectory }

        val pathForQuint: String
        var workspaceRoot: File? = null
        return try {
            if (parentDir != null) {
                workspaceRoot = mirrorWorkspace(parentDir, originalFile.name, collectedInfo.documentText)
                pathForQuint = File(workspaceRoot, originalFile.name).canonicalPath
            } else {
                // Test fixtures use a virtual FS; the mocked toolRunner doesn't read disk.
                pathForQuint = path
            }
            LOG.info("doAnnotate $path: invoking typecheck on $pathForQuint")
            val raw = collectedInfo.toolRunner.typecheck(pathForQuint)
            val result = if (workspaceRoot != null) remapAllSources(raw, workspaceRoot.canonicalPath, parentDir!!.canonicalPath) else raw
            LOG.info("doAnnotate $path: result errors=${result.errors.size} warnings=${result.warnings.size}")
            resultCache[path] = CachedTypecheckResult(collectedInfo.contentHash, result)
            QuintAnnotationResult(collectedInfo.contentHash, result)
        } catch (e: Exception) {
            LOG.warn("doAnnotate $path: typecheck failed: ${e.message}", e)
            null
        }
    }

    /**
     * Build / refresh a private mirror of `sourceDir` under the system tmp dir, then
     * write `targetText` as the snapshot for `targetName`. Sibling .qnt files are hard-
     * linked when possible, copied otherwise. Mirror lives across calls so we only
     * re-link files that changed since the last typecheck.
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

        // Always replace the target with our snapshot. Delete any leftover hard-link
        // (would be the original on-disk content) before writing.
        val target = File(workspace, targetName)
        target.delete()
        target.writeText(targetText, StandardCharsets.UTF_8)
        return workspace
    }

    override fun apply(file: PsiFile, annotationResult: QuintAnnotationResult?, holder: AnnotationHolder) {
        if (annotationResult == null) return

        val originalPsi = file.originalFile
        val virtualFile = originalPsi.virtualFile ?: file.virtualFile ?: return
        val document = FileDocumentManager.getInstance().getDocument(virtualFile) ?: return

        val result = annotationResult.typecheckResult
        var written = 0
        for (error in result.errors) {
            written += applyAnnotation(error, virtualFile.path, document, holder, HighlightSeverity.ERROR)
        }
        for (warning in result.warnings) {
            written += applyAnnotation(warning, virtualFile.path, document, holder, HighlightSeverity.WARNING)
        }
        LOG.info("apply ${virtualFile.path}: wrote $written annotations (errors=${result.errors.size} warnings=${result.warnings.size}) modules=${result.modules.size} types=${result.types.size}")

        // Only refresh the type cache when quint actually returned type info. When
        // typecheck has errors quint returns modules but an empty types map; if we
        // updated from that we'd wipe the previous (good) types and the hover would
        // go blank. Preserve the last good types instead.
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
    ): Int {
        val message = error.explanation.trim()
        if (message.isEmpty()) return 0

        var written = 0
        for (loc in error.locs) {
            if (loc.source != filePath) continue

            val textRange = computeTextRange(loc, document) ?: continue
            holder.newAnnotation(severity, message.substringBefore('\n'))
                .range(textRange)
                .tooltip(message)
                .create()
            written++
        }

        if (error.locs.isEmpty()) {
            holder.newAnnotation(severity, message.substringBefore('\n'))
                .range(TextRange(0, minOf(1, document.textLength)))
                .tooltip(message)
                .create()
            written++
        }
        return written
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
    val contentHash: Int,
    val result: QuintTypecheckResult
)

/** Remap any source path under `fromDir` to the equivalent path under `toDir`. */
private fun remapAllSources(result: QuintTypecheckResult, fromDir: String, toDir: String): QuintTypecheckResult {
    fun remapPath(p: String): String =
        if (p.startsWith(fromDir)) toDir + p.substring(fromDir.length) else p
    fun List<QuintError>.remap() = map { error ->
        error.copy(locs = error.locs.map { loc -> loc.copy(source = remapPath(loc.source)) })
    }
    return result.copy(
        errors = result.errors.remap(),
        warnings = result.warnings.remap()
    )
}
