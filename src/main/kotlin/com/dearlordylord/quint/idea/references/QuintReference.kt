package com.dearlordylord.quint.idea.references

import com.dearlordylord.quint.idea.psi.QuintPsiUtils
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiNamedElement
import com.intellij.psi.PsiReferenceBase

class QuintReference(element: PsiElement, textRange: TextRange) :
    PsiReferenceBase<PsiElement>(element, textRange) {

    override fun resolve(): PsiElement? {
        val name = element.text

        // Handle qualified names (Foo::bar)
        if ("::" in name) {
            return resolveQualified(name)
        }

        val declarations = QuintScopeResolver.findVisibleDeclarations(element)
        return declarations.firstOrNull { it.name == name }
    }

    override fun getVariants(): Array<Any> = emptyArray()

    private fun resolveQualified(qualName: String): PsiElement? {
        val parts = qualName.split("::")
        if (parts.size != 2) return null
        val (moduleName, memberName) = parts

        val file = element.containingFile ?: return null
        val modules = QuintPsiUtils.findModules(file)
        val targetModule = modules.firstOrNull {
            QuintPsiUtils.getDeclarationName(it) == moduleName
        } ?: return null

        val declarations = QuintScopeResolver.findModuleLevelDeclarations(targetModule)
        return declarations.firstOrNull { it.name == memberName }
    }
}
