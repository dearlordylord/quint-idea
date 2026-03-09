package com.dearlordylord.quint.idea.highlighting

import com.dearlordylord.quint.idea.QuintTokenTypes
import com.intellij.lexer.FlexAdapter
import com.intellij.lexer.Lexer
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors
import com.intellij.openapi.editor.HighlighterColors
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.editor.colors.TextAttributesKey.createTextAttributesKey
import com.intellij.openapi.fileTypes.SyntaxHighlighterBase
import com.intellij.psi.tree.IElementType

class QuintSyntaxHighlighter : SyntaxHighlighterBase() {

    companion object {
        val KEYWORD = createTextAttributesKey("QUINT_KEYWORD", DefaultLanguageHighlighterColors.KEYWORD)
        val OPERATOR = createTextAttributesKey("QUINT_OPERATOR", DefaultLanguageHighlighterColors.OPERATION_SIGN)
        val STRING = createTextAttributesKey("QUINT_STRING", DefaultLanguageHighlighterColors.STRING)
        val NUMBER = createTextAttributesKey("QUINT_NUMBER", DefaultLanguageHighlighterColors.NUMBER)
        val LINE_COMMENT = createTextAttributesKey("QUINT_LINE_COMMENT", DefaultLanguageHighlighterColors.LINE_COMMENT)
        val BLOCK_COMMENT = createTextAttributesKey("QUINT_BLOCK_COMMENT", DefaultLanguageHighlighterColors.BLOCK_COMMENT)
        val DOC_COMMENT = createTextAttributesKey("QUINT_DOC_COMMENT", DefaultLanguageHighlighterColors.DOC_COMMENT)
        val IDENTIFIER = createTextAttributesKey("QUINT_IDENTIFIER", DefaultLanguageHighlighterColors.IDENTIFIER)
        val CLASS_NAME = createTextAttributesKey("QUINT_CLASS_NAME", DefaultLanguageHighlighterColors.CLASS_NAME)
        val BRACES = createTextAttributesKey("QUINT_BRACES", DefaultLanguageHighlighterColors.BRACES)
        val BRACKETS = createTextAttributesKey("QUINT_BRACKETS", DefaultLanguageHighlighterColors.BRACKETS)
        val PARENTHESES = createTextAttributesKey("QUINT_PARENTHESES", DefaultLanguageHighlighterColors.PARENTHESES)
        val COMMA = createTextAttributesKey("QUINT_COMMA", DefaultLanguageHighlighterColors.COMMA)
        val SEMICOLON = createTextAttributesKey("QUINT_SEMICOLON", DefaultLanguageHighlighterColors.SEMICOLON)
        val DOT = createTextAttributesKey("QUINT_DOT", DefaultLanguageHighlighterColors.DOT)
        val BAD_CHARACTER = createTextAttributesKey("QUINT_BAD_CHARACTER", HighlighterColors.BAD_CHARACTER)

        private val KEYWORD_KEYS = arrayOf(KEYWORD)
        private val OPERATOR_KEYS = arrayOf(OPERATOR)
        private val STRING_KEYS = arrayOf(STRING)
        private val NUMBER_KEYS = arrayOf(NUMBER)
        private val LINE_COMMENT_KEYS = arrayOf(LINE_COMMENT)
        private val BLOCK_COMMENT_KEYS = arrayOf(BLOCK_COMMENT)
        private val DOC_COMMENT_KEYS = arrayOf(DOC_COMMENT)
        private val IDENTIFIER_KEYS = arrayOf(IDENTIFIER)
        private val CLASS_NAME_KEYS = arrayOf(CLASS_NAME)
        private val BRACES_KEYS = arrayOf(BRACES)
        private val BRACKETS_KEYS = arrayOf(BRACKETS)
        private val PARENTHESES_KEYS = arrayOf(PARENTHESES)
        private val COMMA_KEYS = arrayOf(COMMA)
        private val SEMICOLON_KEYS = arrayOf(SEMICOLON)
        private val DOT_KEYS = arrayOf(DOT)
        private val BAD_CHARACTER_KEYS = arrayOf(BAD_CHARACTER)
        private val EMPTY = emptyArray<TextAttributesKey>()
    }

    override fun getHighlightingLexer(): Lexer =
        FlexAdapter(com.dearlordylord.quint.idea.lexer.QuintLexer(null as java.io.Reader?))

    override fun getTokenHighlights(tokenType: IElementType): Array<TextAttributesKey> = when (tokenType) {
        // Keywords
        QuintTokenTypes.MODULE, QuintTokenTypes.IMPORT, QuintTokenTypes.EXPORT,
        QuintTokenTypes.FROM, QuintTokenTypes.AS, QuintTokenTypes.CONST,
        QuintTokenTypes.VAR, QuintTokenTypes.ASSUME, QuintTokenTypes.TYPE,
        QuintTokenTypes.VAL, QuintTokenTypes.DEF, QuintTokenTypes.PURE,
        QuintTokenTypes.ACTION, QuintTokenTypes.RUN, QuintTokenTypes.TEMPORAL,
        QuintTokenTypes.NONDET, QuintTokenTypes.IF, QuintTokenTypes.ELSE,
        QuintTokenTypes.MATCH, QuintTokenTypes.AND, QuintTokenTypes.OR,
        QuintTokenTypes.ALL, QuintTokenTypes.ANY, QuintTokenTypes.IFF,
        QuintTokenTypes.IMPLIES, QuintTokenTypes.BOOL -> KEYWORD_KEYS

        // Type keywords
        QuintTokenTypes.SET, QuintTokenTypes.LIST -> CLASS_NAME_KEYS

        // Operators
        QuintTokenTypes.PLUS, QuintTokenTypes.MINUS, QuintTokenTypes.MUL,
        QuintTokenTypes.DIV, QuintTokenTypes.MOD, QuintTokenTypes.POW,
        QuintTokenTypes.GT, QuintTokenTypes.LT, QuintTokenTypes.GE,
        QuintTokenTypes.LE, QuintTokenTypes.NE, QuintTokenTypes.EQ,
        QuintTokenTypes.ASGN, QuintTokenTypes.ARROW, QuintTokenTypes.FAT_ARROW,
        QuintTokenTypes.PIPE, QuintTokenTypes.SPREAD, QuintTokenTypes.COLONCOLON -> OPERATOR_KEYS

        // Literals
        QuintTokenTypes.STRING -> STRING_KEYS
        QuintTokenTypes.INT -> NUMBER_KEYS

        // Comments
        QuintTokenTypes.LINE_COMMENT -> LINE_COMMENT_KEYS
        QuintTokenTypes.COMMENT -> BLOCK_COMMENT_KEYS
        QuintTokenTypes.DOCCOMMENT -> DOC_COMMENT_KEYS

        // Identifiers
        QuintTokenTypes.LOW_ID -> IDENTIFIER_KEYS
        QuintTokenTypes.CAP_ID -> CLASS_NAME_KEYS

        // Braces / brackets / parens
        QuintTokenTypes.LBRACE, QuintTokenTypes.RBRACE -> BRACES_KEYS
        QuintTokenTypes.LBRACKET, QuintTokenTypes.RBRACKET -> BRACKETS_KEYS
        QuintTokenTypes.LPAREN, QuintTokenTypes.RPAREN -> PARENTHESES_KEYS

        // Punctuation
        QuintTokenTypes.COMMA -> COMMA_KEYS
        QuintTokenTypes.SEMICOLON -> SEMICOLON_KEYS
        QuintTokenTypes.DOT -> DOT_KEYS

        // Bad character
        QuintTokenTypes.BAD_CHARACTER -> BAD_CHARACTER_KEYS

        else -> EMPTY
    }
}
