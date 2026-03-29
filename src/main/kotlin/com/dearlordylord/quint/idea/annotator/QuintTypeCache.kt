package com.dearlordylord.quint.idea.annotator

import com.intellij.openapi.util.Key
import com.intellij.openapi.vfs.VirtualFile

/**
 * Per-file cache for type information from quint typecheck.
 * Populated by the external annotator, consumed by the documentation provider.
 */
object QuintTypeCache {

    private val TYPE_DATA_KEY = Key.create<Map<DeclKey, QuintTypeScheme>>("QUINT_TYPE_DATA")

    fun update(file: VirtualFile, result: QuintTypecheckResult) {
        val declTypes = mutableMapOf<DeclKey, QuintTypeScheme>()

        for (module in result.modules) {
            for (decl in module.declarations) {
                val typeScheme = result.types[decl.id.toString()]
                if (typeScheme != null) {
                    declTypes[DeclKey(module.name, decl.name)] = typeScheme
                }
            }
        }

        file.putUserData(TYPE_DATA_KEY, declTypes)
    }

    fun getTypeScheme(file: VirtualFile, moduleName: String, declName: String): QuintTypeScheme? {
        val data = file.getUserData(TYPE_DATA_KEY) ?: return null
        return data[DeclKey(moduleName, declName)]
    }

    fun getFormattedType(file: VirtualFile, moduleName: String, declName: String): String? {
        val scheme = getTypeScheme(file, moduleName, declName) ?: return null
        return QuintTypeFormatter.formatScheme(scheme)
    }

    private data class DeclKey(val moduleName: String, val declName: String)
}
