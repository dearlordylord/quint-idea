package com.dearlordylord.quint.idea.annotator

import com.intellij.openapi.application.PathManager
import com.intellij.openapi.util.io.FileUtil
import java.io.File
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.StandardCopyOption

data class QuintFileSnapshot(
    val filePath: String,
    val text: String
)

class QuintTypecheckExecutor(private val runner: QuintToolRunner) {

    fun typecheck(snapshot: QuintFileSnapshot): QuintTypecheckResult? {
        val originalFile = File(snapshot.filePath)
        val parentDir = originalFile.parentFile ?: return null

        val workspaceRoot = mirrorWorkspace(parentDir, originalFile.name, snapshot.text)
        val workspacePath = workspaceRoot.canonicalPath
        val pathForQuint = File(workspaceRoot, originalFile.name).canonicalPath
        val raw = runner.typecheck(pathForQuint)
        return remapAllSources(raw, workspacePath, parentDir.canonicalPath)
    }

    /**
     * Hard-links (or copies) sibling .qnt files from [sourceDir] into a stable mirror
     * under the system tmp dir so relative imports resolve, then writes [targetText] as
     * the snapshot for [targetName]. Keeps nothing in the user's source dir.
     */
    private fun mirrorWorkspace(sourceDir: File, targetName: String, targetText: String): File {
        // PathManager.getTempPath is the plugin-sanctioned temp location; FileUtil.pathHashCode
        // normalizes case on case-insensitive filesystems so two paths to the same dir map here.
        val workspace = File(
            PathManager.getTempPath(),
            "quint-idea-${Integer.toHexString(FileUtil.pathHashCode(sourceDir.canonicalPath))}"
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
}
