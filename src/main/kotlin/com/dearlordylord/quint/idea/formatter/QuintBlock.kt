package com.dearlordylord.quint.idea.formatter

import com.dearlordylord.quint.idea.parser.QuintParserTokenTypes
import com.intellij.formatting.*
import com.intellij.lang.ASTNode
import com.intellij.psi.TokenType
import com.intellij.psi.formatter.common.AbstractBlock
import com.intellij.psi.tree.IElementType

class QuintBlock(
    node: ASTNode,
    wrap: Wrap?,
    alignment: Alignment?,
    private val spacingBuilder: SpacingBuilder,
    private val indent: Indent = Indent.getNoneIndent()
) : AbstractBlock(node, wrap, alignment) {

    companion object {
        val LBRACE: IElementType = QuintParserTokenTypes.LBRACE
        val RBRACE: IElementType = QuintParserTokenTypes.RBRACE
        val LBRACKET: IElementType = QuintParserTokenTypes.LBRACKET
        val RBRACKET: IElementType = QuintParserTokenTypes.RBRACKET
        val LPAREN: IElementType = QuintParserTokenTypes.LPAREN
        val RPAREN: IElementType = QuintParserTokenTypes.RPAREN

        private val DELIMITERS = QuintParserTokenTypes.DELIMITERS
    }

    override fun buildChildren(): List<Block> {
        // Single pass: compute delimiter nesting state for all children
        var inBraces = false
        var inBrackets = false
        var inParens = false
        val blocks = mutableListOf<Block>()
        var child = myNode.firstChildNode
        while (child != null) {
            val type = child.elementType
            when (type) {
                LBRACE -> inBraces = true
                RBRACE -> inBraces = false
                LBRACKET -> inBrackets = true
                RBRACKET -> inBrackets = false
                LPAREN -> inParens = true
                RPAREN -> inParens = false
            }
            if (type != TokenType.WHITE_SPACE) {
                val childIndent = when {
                    type in DELIMITERS -> Indent.getNoneIndent()
                    inBraces || inBrackets -> Indent.getNormalIndent()
                    inParens -> Indent.getContinuationIndent()
                    else -> Indent.getNoneIndent()
                }
                blocks.add(QuintBlock(child, null, null, spacingBuilder, childIndent))
            }
            child = child.treeNext
        }
        return blocks
    }

    override fun getIndent(): Indent = indent

    override fun getSpacing(child1: Block?, child2: Block): Spacing? {
        return spacingBuilder.getSpacing(this, child1, child2)
    }

    override fun getChildAttributes(newChildIndex: Int): ChildAttributes {
        var node = myNode.firstChildNode
        while (node != null) {
            val type = node.elementType
            if (type == LBRACE || type == LBRACKET) {
                return ChildAttributes(Indent.getNormalIndent(), null)
            }
            node = node.treeNext
        }
        return ChildAttributes(Indent.getNoneIndent(), null)
    }

    override fun isLeaf(): Boolean = myNode.firstChildNode == null
}
