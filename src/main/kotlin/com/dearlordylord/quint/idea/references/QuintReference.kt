package com.dearlordylord.quint.idea.references

import com.dearlordylord.quint.idea.psi.QuintPsiUtils
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReferenceBase
import com.intellij.util.IncorrectOperationException

class QuintReference(element: PsiElement, textRange: TextRange) :
    PsiReferenceBase<PsiElement>(element, textRange) {

    override fun resolve(): PsiElement? {
        return QuintNameResolver.resolve(element)
    }

    override fun handleElementRename(newElementName: String): PsiElement {
        val currentText = element.text
        val newText = if ("::" in currentText) {
            val prefix = currentText.substringBeforeLast("::")
            "$prefix::$newElementName"
        } else {
            newElementName
        }
        val newId = QuintPsiUtils.createQualIdFromText(element.project, newText)
            ?: throw IncorrectOperationException("Cannot create identifier")
        element.node.treeParent.replaceChild(element.node, newId.node)
        return newId
    }

    override fun getVariants(): Array<Any> = emptyArray()
}
