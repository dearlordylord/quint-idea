package com.dearlordylord.quint.idea.editor

import com.intellij.lang.ASTNode
import com.intellij.lang.folding.FoldingBuilderEx
import com.intellij.lang.folding.FoldingDescriptor
import com.intellij.openapi.editor.Document
import com.intellij.psi.PsiElement

class QuintFoldingBuilder : FoldingBuilderEx() {
    override fun buildFoldRegions(root: PsiElement, document: Document, quick: Boolean): Array<FoldingDescriptor> =
        emptyArray()

    override fun getPlaceholderText(node: ASTNode): String = "..."
    override fun isCollapsedByDefault(node: ASTNode): Boolean = false
}
