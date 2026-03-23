package com.dearlordylord.quint.idea.psi

import com.dearlordylord.quint.idea.references.QuintFileReference
import com.intellij.lang.ASTNode
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiReference
import org.antlr.intellij.adaptor.psi.ANTLRPsiNode

/**
 * PSI node for the `fromSource` rule (`fromSource: STRING`).
 * Provides a file reference so Cmd+Click on the string navigates to the target file.
 */
class QuintFromSourceNode(node: ASTNode) : ANTLRPsiNode(node) {

    override fun getReference(): PsiReference? {
        val text = text ?: return null
        if (text.length < 2 || !text.startsWith("\"") || !text.endsWith("\"")) return null
        val range = TextRange(1, text.length - 1)
        return QuintFileReference(this, range)
    }
}
