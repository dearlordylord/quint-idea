package com.dearlordylord.quint.idea.references

import com.dearlordylord.quint.idea.psi.QuintPsiShape
import com.dearlordylord.quint.idea.psi.QuintPsiUtils
import com.dearlordylord.quint.idea.parser.QuintParser
import com.intellij.psi.PsiElement

object QuintNameResolver {

    fun resolve(element: PsiElement): PsiElement? {
        val name = element.text

        resolveInstanceParam(element, name)?.let { return it }
        QuintScopeResolver.findVisibleSymbols(element).firstOrNull { it.name == name }?.let { return it.declaration }
        if ("::" in name) {
            val file = element.containingFile ?: return null
            for (module in QuintPsiUtils.findModules(file)) {
                val prefix = QuintPsiUtils.getDeclarationName(module) ?: continue
                if (name.startsWith("$prefix::")) {
                    QuintScopeResolver.exportedSymbols(module).firstOrNull { it.name == name.removePrefix("$prefix::") }?.let { return it.declaration }
                }
            }
        }
        return QuintRecordTypeResolver.resolveNameAfterDotField(element, name)
    }

    fun resolveMemberInModule(context: PsiElement, moduleName: String, memberName: String): PsiElement? {
        val file = context.containingFile ?: return null
        val containing = QuintPsiUtils.getContainingModule(context)
        val imported = containing?.let { QuintImportResolver.findImportsInModule(it).firstOrNull { imp -> (imp.alias ?: imp.moduleName) == moduleName } }
        val target = QuintImportResolver.findModule(imported?.moduleName ?: moduleName, imported?.fromSource, file) ?: return null
        return QuintScopeResolver.exportedSymbols(target).firstOrNull { it.name == memberName }?.declaration
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

}
