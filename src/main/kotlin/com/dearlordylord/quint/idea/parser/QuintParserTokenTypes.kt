package com.dearlordylord.quint.idea.parser

import com.intellij.psi.tree.IElementType

object QuintParserTokenTypes {
    private val tokenTypes = QuintParserDefinition.TOKEN_ELEMENT_TYPES

    val LBRACE: IElementType = tokenTypes[QuintLexer.T__1]
    val RBRACE: IElementType = tokenTypes[QuintLexer.T__2]
    val COLON: IElementType = tokenTypes[QuintLexer.T__4]
    val COMMA: IElementType = tokenTypes[QuintLexer.T__7]
    val LBRACKET: IElementType = tokenTypes[QuintLexer.T__12]
    val RBRACKET: IElementType = tokenTypes[QuintLexer.T__13]
    val ARROW: IElementType = tokenTypes[QuintLexer.T__21]
    val FAT_ARROW: IElementType = tokenTypes[QuintLexer.T__22]

    val LPAREN: IElementType = tokenTypes[QuintLexer.LPAREN]
    val RPAREN: IElementType = tokenTypes[QuintLexer.RPAREN]

    val EQ: IElementType = tokenTypes[QuintLexer.EQ]
    val NE: IElementType = tokenTypes[QuintLexer.NE]
    val GE: IElementType = tokenTypes[QuintLexer.GE]
    val LE: IElementType = tokenTypes[QuintLexer.LE]
    val ASGN: IElementType = tokenTypes[QuintLexer.ASGN]
    val PLUS: IElementType = tokenTypes[QuintLexer.PLUS]
    val MINUS: IElementType = tokenTypes[QuintLexer.MINUS]
    val MUL: IElementType = tokenTypes[QuintLexer.MUL]
    val DIV: IElementType = tokenTypes[QuintLexer.DIV]
    val MOD: IElementType = tokenTypes[QuintLexer.MOD]
    val GT: IElementType = tokenTypes[QuintLexer.GT]
    val LT: IElementType = tokenTypes[QuintLexer.LT]

    val DELIMITERS = setOf(LBRACE, RBRACE, LBRACKET, RBRACKET, LPAREN, RPAREN)
}
