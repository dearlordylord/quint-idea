package com.dearlordylord.quint.idea.annotator

import com.intellij.openapi.util.Key
import com.intellij.openapi.vfs.VirtualFile

/**
 * Per-file cache for type information from quint typecheck.
 * Populated by the external annotator, consumed by the documentation provider.
 */
object QuintTypeCache {

    private data class CachedTypes(val snapshot: QuintAnalysisSnapshot?, val declarations: Map<DeclKey, QuintTypeScheme>)
    private val TYPE_DATA_KEY = Key.create<CachedTypes>("QUINT_TYPE_DATA")

    fun update(file: VirtualFile, result: QuintTypecheckResult, snapshot: QuintAnalysisSnapshot? = null) {
        val declTypes = mutableMapOf<DeclKey, QuintTypeScheme>()

        for (module in result.modules) {
            for (decl in module.declarations) {
                val typeScheme = result.types[decl.id.toString()]
                if (typeScheme != null) {
                    declTypes[DeclKey(module.name, decl.name)] = typeScheme
                } else if (decl.kind == "typedef" && decl.type != null) {
                    // typedef declarations store their type inline, not in the types map
                    declTypes[DeclKey(module.name, decl.name)] = QuintTypeScheme(
                        typeVariables = emptyList(),
                        rowVariables = emptyList(),
                        type = decl.type
                    )
                }
            }
        }

        file.putUserData(TYPE_DATA_KEY, CachedTypes(snapshot, declTypes.toMap()))
    }

    fun getTypeScheme(file: VirtualFile, moduleName: String, declName: String): QuintTypeScheme? {
        val entry = file.getUserData(TYPE_DATA_KEY) ?: return null
        val snapshot = entry.snapshot
        if (snapshot != null) {
            val text = com.intellij.openapi.fileEditor.FileDocumentManager.getInstance().getDocument(file)?.text ?: return null
            if (!snapshot.isCurrent(text)) return null
        }
        return entry.declarations[DeclKey(moduleName, declName)]
    }

    fun getFormattedType(file: VirtualFile, moduleName: String, declName: String): String? {
        val scheme = getTypeScheme(file, moduleName, declName) ?: return null
        return QuintTypeFormatter.formatScheme(scheme)
    }

    private data class DeclKey(val moduleName: String, val declName: String)
}
