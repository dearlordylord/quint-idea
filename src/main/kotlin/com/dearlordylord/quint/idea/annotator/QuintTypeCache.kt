package com.dearlordylord.quint.idea.annotator

import com.intellij.openapi.util.Key
import com.intellij.openapi.vfs.VirtualFile

/**
 * Per-file cache for type information from quint typecheck.
 * Populated by the external annotator, consumed by the documentation provider.
 */
object QuintTypeCache {

    private val TYPE_DATA_KEY = Key.create<Map<DeclKey, QuintTypeScheme>>("QUINT_TYPE_DATA")

    private val SNAPSHOT_KEY = Key.create<QuintAnalysisSnapshot>("QUINT_TYPE_SNAPSHOT")

    fun update(file: VirtualFile, result: QuintTypecheckResult, snapshot: QuintAnalysisSnapshot? = null) {
        file.putUserData(SNAPSHOT_KEY, snapshot)
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

        file.putUserData(TYPE_DATA_KEY, declTypes)
    }

    fun getTypeScheme(file: VirtualFile, moduleName: String, declName: String): QuintTypeScheme? {
        val snapshot = file.getUserData(SNAPSHOT_KEY)
        if (snapshot != null) {
            val text = com.intellij.openapi.fileEditor.FileDocumentManager.getInstance().getDocument(file)?.text ?: return null
            if (!snapshot.isCurrent(text)) return null
        }
        val data = file.getUserData(TYPE_DATA_KEY) ?: return null
        return data[DeclKey(moduleName, declName)]
    }

    fun getFormattedType(file: VirtualFile, moduleName: String, declName: String): String? {
        val scheme = getTypeScheme(file, moduleName, declName) ?: return null
        return QuintTypeFormatter.formatScheme(scheme)
    }

    private data class DeclKey(val moduleName: String, val declName: String)
}
