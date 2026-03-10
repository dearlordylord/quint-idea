package com.dearlordylord.quint.idea.psi

import com.dearlordylord.quint.idea.references.QuintReference
import com.intellij.lang.ASTNode
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReference
import com.intellij.psi.PsiReferenceBase
import com.intellij.psi.util.PsiTreeUtil
import org.antlr.intellij.adaptor.psi.ANTLRPsiNode

class QuintQualIdNode(node: ASTNode) : ANTLRPsiNode(node) {

    override fun getReference(): PsiReference? {
        if (QuintPsiUtils.isDeclarationIdentifier(this)) {
            // Return a reference that resolves to the parent QuintNamedElement.
            // This lets TargetElementUtil find the rename target at declaration sites.
            // isReferenceTo returns false so Find Usages doesn't count the declaration as a usage.
            val named = PsiTreeUtil.getParentOfType(this, QuintNamedElement::class.java)
                ?: return null
            return object : PsiReferenceBase<PsiElement>(this, TextRange(0, textLength), true) {
                override fun resolve(): PsiElement = named
                override fun isReferenceTo(element: PsiElement): Boolean = false
            }
        }
        return QuintReference(this, TextRange(0, textLength))
    }
}
