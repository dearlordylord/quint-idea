package com.dearlordylord.quint.idea.psi

import com.dearlordylord.quint.idea.references.QuintReference
import com.intellij.lang.ASTNode
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiReference
import org.antlr.intellij.adaptor.psi.ANTLRPsiNode

class QuintQualIdNode(node: ASTNode) : ANTLRPsiNode(node) {

    override fun getReference(): PsiReference? {
        if (QuintPsiUtils.isDeclarationIdentifier(this)) return null
        return QuintReference(this, TextRange(0, textLength))
    }
}
