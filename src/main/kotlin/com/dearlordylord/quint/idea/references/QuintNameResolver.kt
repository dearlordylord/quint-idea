package com.dearlordylord.quint.idea.references

import com.dearlordylord.quint.idea.psi.QuintPsiShape
import com.dearlordylord.quint.idea.psi.QuintPsiUtils
import com.dearlordylord.quint.idea.parser.QuintParser
import com.intellij.psi.PsiElement

object QuintNameResolver {

    fun resolve(element: PsiElement): PsiElement? {
        val name = element.text

        if ("::" in name) {
            return resolveQualified(element, name)
        }

        resolveInstanceParam(element, name)?.let { return it }

        val scopeResult = QuintScopeResolver.findVisibleDeclarations(element)
            .firstOrNull { it.name == name }
        if (scopeResult != null) return scopeResult

        return resolveAsRecordField(element, name)
    }

    fun resolveMemberInModule(context: PsiElement, moduleName: String, memberName: String): PsiElement? {
        val file = context.containingFile ?: return null

        val sameFileModule = QuintPsiUtils.findModules(file).firstOrNull {
            QuintPsiUtils.getDeclarationName(it) == moduleName
        }
        if (sameFileModule != null) {
            val decl = QuintScopeResolver.findModuleLevelDeclarations(sameFileModule)
                .firstOrNull { it.name == memberName }
            if (decl != null) return decl
        }

        val containingModule = QuintPsiUtils.getContainingModule(context) ?: return null
        val imports = QuintImportResolver.findImportsInModule(containingModule)
        for (imp in imports) {
            val matches = when (imp.kind) {
                ImportKind.QUALIFIED -> imp.moduleName == moduleName
                ImportKind.ALIASED -> imp.alias == moduleName
                else -> false
            }
            if (!matches) continue
            val targetModule = QuintImportResolver.findModule(imp.moduleName, imp.fromSource, file)
                ?: continue
            val decl = QuintScopeResolver.findModuleLevelDeclarations(targetModule)
                .firstOrNull { it.name == memberName }
            if (decl != null) return decl
        }
        return null
    }

    private fun resolveAsRecordField(element: PsiElement, name: String): PsiElement? {
        return QuintRecordTypeResolver.resolveNameAfterDotField(element, name)
    }

    private fun resolveInstanceParam(element: PsiElement, name: String): PsiElement? {
        val nameNode = element.parent ?: return null
        if (!QuintPsiShape.isNameNode(nameNode)) return null

        val instanceMod = nameNode.parent ?: return null
        if (!QuintPsiShape.isInstanceModule(instanceMod)) return null

        val moduleNameNode = QuintPsiUtils.findFirstChildOfRule(instanceMod, QuintParser.RULE_moduleName)
            ?: return null
        return resolveMemberInModule(element, moduleNameNode.text, name)
    }

    private fun resolveQualified(element: PsiElement, qualName: String): PsiElement? {
        val parts = qualName.split("::")
        if (parts.size != 2) return null
        return resolveMemberInModule(element, parts[0], parts[1])
    }
}
