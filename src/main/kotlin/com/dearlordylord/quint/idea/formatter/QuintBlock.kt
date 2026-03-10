package com.dearlordylord.quint.idea.formatter

import com.dearlordylord.quint.idea.parser.QuintLexer
import com.dearlordylord.quint.idea.parser.QuintParserDefinition
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
        val LBRACE: IElementType = QuintParserDefinition.TOKEN_ELEMENT_TYPES[QuintLexer.T__1]   // {
        val RBRACE: IElementType = QuintParserDefinition.TOKEN_ELEMENT_TYPES[QuintLexer.T__2]   // }
        val LBRACKET: IElementType = QuintParserDefinition.TOKEN_ELEMENT_TYPES[QuintLexer.T__12] // [
        val RBRACKET: IElementType = QuintParserDefinition.TOKEN_ELEMENT_TYPES[QuintLexer.T__13] // ]
        val LPAREN: IElementType = QuintParserDefinition.TOKEN_ELEMENT_TYPES[QuintLexer.LPAREN]
        val RPAREN: IElementType = QuintParserDefinition.TOKEN_ELEMENT_TYPES[QuintLexer.RPAREN]

        private val DELIMITERS = setOf(LBRACE, RBRACE, LBRACKET, RBRACKET, LPAREN, RPAREN)
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
