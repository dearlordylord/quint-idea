package com.dearlordylord.quint.idea.annotator

import com.intellij.openapi.application.PathManager
import java.io.File
import java.nio.file.Files

/** Compatibility input for callers checking a single persisted import graph. */
data class QuintFileSnapshot(val filePath: String, val text: String)

class QuintTypecheckExecutor(private val runner: QuintToolRunner) {
    fun typecheck(snapshot: QuintFileSnapshot): QuintTypecheckResult? =
        typecheck(QuintAnalysisSnapshot.capture(snapshot.filePath, snapshot.text, null))

    fun typecheck(snapshot: QuintAnalysisSnapshot): QuintTypecheckResult {
        val temp = File(PathManager.getTempPath()).apply { mkdirs() }
        val workspace = Files.createTempDirectory(temp.toPath(), "quint-check-").toFile()
        try {
            // Preserve the full relative layout, including missing paths, under one common ancestor.
            var ancestor = File(snapshot.rootPath).parentFile.toPath()
            for (path in snapshot.identities.keys) {
                while (!File(path).toPath().startsWith(ancestor)) ancestor = ancestor.parent
            }
            val paths = snapshot.identities.keys.associateWith { original ->
                File(workspace, ancestor.relativize(File(original).toPath()).toString())
            }
            for ((original, text) in snapshot.sources) {
                val target = paths.getValue(original)
                target.parentFile.mkdirs()
                target.writeText(text)
            }
            val originals = paths.entries.associate { it.value.absolutePath to it.key }
            val raw = runner.typecheck(paths.getValue(snapshot.rootPath).absolutePath)
            fun List<QuintError>.remap() = map { error ->
                error.copy(locs = error.locs.map { loc ->
                    loc.copy(source = originals[File(loc.source).absoluteFile.normalize().path] ?: loc.source)
                })
            }
            return raw.copy(errors = raw.errors.remap(), warnings = raw.warnings.remap())
        } finally {
            workspace.deleteRecursively()
        }
    }
}
