package com.dearlordylord.quint.idea.annotator

import com.dearlordylord.quint.idea.parser.QuintLexer
import com.dearlordylord.quint.idea.parser.QuintParser
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.vfs.VirtualFileManager
import org.antlr.v4.runtime.CharStreams
import org.antlr.v4.runtime.CommonTokenStream
import org.antlr.v4.runtime.tree.ParseTree
import java.io.File
import java.security.MessageDigest

/** Immutable source input; import observations include absent files and symlink targets. */
data class QuintAnalysisSnapshot(
    val rootPath: String,
    val sources: Map<String, String>,
    val identities: Map<String, String>,
    val executable: String?,
    val executableStamp: String,
    val sourceProtocol: String = "file"
) {
    fun isCurrent(rootText: String, currentToolStamp: String = toolStamp(com.dearlordylord.quint.idea.settings.QuintSettingsState.getInstance().resolveQuintPath())): Boolean =
        sources[rootPath] == rootText &&
            executableStamp == currentToolStamp &&
            identities.all { (path, identity) -> File(path).canonicalPath == identity &&
                (if (path == rootPath) rootText else readCurrentSource(path, sourceProtocol)) == sources[path] }

    companion object {
        fun capture(path: String, text: String, executable: String?, sourceProtocol: String = "file"): QuintAnalysisSnapshot {
            val root = File(path).absoluteFile.normalize().path
            val sources = linkedMapOf(root to text)
            val identities = linkedMapOf<String, String>()
            val pending = ArrayDeque<String>().apply { add(root) }
            while (pending.isNotEmpty()) {
                ProgressManager.checkCanceled()
                val source = pending.removeFirst()
                if (source in identities) continue
                require(identities.size < 2000) { "Quint import graph exceeds 2000 files" }
                identities[source] = File(source).canonicalPath
                val contents = sources[source] ?: readCurrentSource(source, sourceProtocol) ?: continue
                sources[source] = contents
                for (import in imports(contents)) {
                    // The Quint loader always appends .qnt, even when the spelling includes it.
                    val target = File(File(source).parentFile, "$import.qnt").absoluteFile.normalize().path
                    if (target !in identities) pending.add(target)
                }
            }
            return QuintAnalysisSnapshot(root, sources.toMap(), identities.toMap(), executable, toolStamp(executable), sourceProtocol)
        }

        private fun imports(text: String): List<String> {
            val lexer = QuintLexer(CharStreams.fromString(text)).apply { removeErrorListeners() }
            val parser = QuintParser(CommonTokenStream(lexer)).apply { removeErrorListeners() }
            val result = mutableListOf<String>()
            fun visit(tree: ParseTree) {
                if (tree is QuintParser.FromSourceContext) {
                    tree.STRING()?.text?.removeSurrounding("\"")?.let(result::add)
                } else for (i in 0 until tree.childCount) visit(tree.getChild(i))
            }
            visit(parser.modules())
            return result
        }

        internal fun readCurrentSource(path: String, protocol: String = "file"): String? {
            val vf = VirtualFileManager.getInstance().getFileSystem(protocol)?.findFileByPath(path)
            val document = vf?.let { FileDocumentManager.getInstance().getCachedDocument(it) }
            if (document != null && FileDocumentManager.getInstance().isDocumentUnsaved(document)) return document.text
            if (protocol != "file") return vf?.takeIf { it.isValid && !it.isDirectory }?.contentsToByteArray()?.toString(Charsets.UTF_8)
            val file = File(path)
            return if (file.isFile) file.readText() else null
        }

        internal fun toolStamp(path: String?): String {
            if (path == null) return "unavailable"
            val file = File(path)
            if (!file.isFile) return "$path:missing"
            val digest = MessageDigest.getInstance("SHA-256")
            file.inputStream().use { input ->
                val buffer = ByteArray(8192)
                while (true) {
                    val n = input.read(buffer)
                    if (n < 0) break
                    digest.update(buffer, 0, n)
                }
            }
            return "${file.canonicalPath}:${file.canExecute()}:${digest.digest().joinToString("") { "%02x".format(it) }}"
        }
    }
}
