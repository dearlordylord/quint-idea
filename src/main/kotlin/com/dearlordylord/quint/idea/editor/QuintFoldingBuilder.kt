package com.dearlordylord.quint.idea.editor

import com.dearlordylord.quint.idea.QuintLanguage
import com.dearlordylord.quint.idea.parser.QuintParser
import com.intellij.lang.ASTNode
import com.intellij.lang.folding.FoldingBuilderEx
import com.intellij.lang.folding.FoldingDescriptor
import com.intellij.openapi.editor.Document
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import org.antlr.intellij.adaptor.lexer.PSIElementTypeFactory
import org.antlr.intellij.adaptor.lexer.RuleIElementType
import org.antlr.intellij.adaptor.lexer.TokenIElementType

class QuintFoldingBuilder : FoldingBuilderEx() {

    override fun buildFoldRegions(root: PsiElement, document: Document, quick: Boolean): Array<FoldingDescriptor> {
        val descriptors = mutableListOf<FoldingDescriptor>()
        collectFoldRegions(root, document, descriptors)
        return descriptors.toTypedArray()
    }

    private fun collectFoldRegions(
        element: PsiElement,
        document: Document,
        descriptors: MutableList<FoldingDescriptor>
    ) {
        val nodeType = element.node?.elementType

        if (nodeType is RuleIElementType) {
            when (nodeType.ruleIndex) {
                // Module bodies: module Name { ... }
                QuintParser.RULE_module -> {
                    addBraceFoldRegion(element, document, descriptors)
                }
                // Block expressions: and{...}, or{...}, all{...}, any{...}
                QuintParser.RULE_expr -> {
                    addBraceFoldRegion(element, document, descriptors)
                }
            }
        }

        // Recurse into children
        var child = element.firstChild
        while (child != null) {
            collectFoldRegions(child, document, descriptors)
            child = child.nextSibling
        }
    }

    /**
     * Finds LBRACE...RBRACE within an element and creates a fold region if it spans multiple lines.
     */
    private fun addBraceFoldRegion(
        element: PsiElement,
        document: Document,
        descriptors: MutableList<FoldingDescriptor>
    ) {
        var lbrace: PsiElement? = null
        var rbrace: PsiElement? = null

        var child = element.firstChild
        while (child != null) {
            val text = child.text
            if (text == "{" && lbrace == null) {
                lbrace = child
            }
            if (text == "}") {
                rbrace = child
            }
            child = child.nextSibling
        }

        if (lbrace != null && rbrace != null) {
            val startOffset = lbrace.textRange.startOffset
            val endOffset = rbrace.textRange.endOffset
            val startLine = document.getLineNumber(startOffset)
            val endLine = document.getLineNumber(endOffset)

            if (endLine > startLine && endOffset > startOffset + 1) {
                descriptors.add(
                    FoldingDescriptor(
                        element.node,
                        TextRange(startOffset, endOffset)
                    )
                )
            }
        }
    }

    override fun getPlaceholderText(node: ASTNode): String = "{...}"

    override fun isCollapsedByDefault(node: ASTNode): Boolean = false
}
