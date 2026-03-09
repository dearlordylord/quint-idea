package com.dearlordylord.quint.idea

import com.intellij.psi.tree.IElementType
import com.intellij.psi.tree.TokenSet

object QuintTokenTypes {
    @JvmField val COMMENT = IElementType("COMMENT", QuintLanguage.INSTANCE)
    @JvmField val LINE_COMMENT = IElementType("LINE_COMMENT", QuintLanguage.INSTANCE)
    @JvmField val DOCCOMMENT = IElementType("DOCCOMMENT", QuintLanguage.INSTANCE)
    @JvmField val STRING = IElementType("STRING", QuintLanguage.INSTANCE)
    @JvmField val INT = IElementType("INT", QuintLanguage.INSTANCE)
    @JvmField val BOOL = IElementType("BOOL", QuintLanguage.INSTANCE)

    // Keywords
    @JvmField val MODULE = IElementType("module", QuintLanguage.INSTANCE)
    @JvmField val IMPORT = IElementType("import", QuintLanguage.INSTANCE)
    @JvmField val EXPORT = IElementType("export", QuintLanguage.INSTANCE)
    @JvmField val FROM = IElementType("from", QuintLanguage.INSTANCE)
    @JvmField val AS = IElementType("as", QuintLanguage.INSTANCE)
    @JvmField val CONST = IElementType("const", QuintLanguage.INSTANCE)
    @JvmField val VAR = IElementType("var", QuintLanguage.INSTANCE)
    @JvmField val ASSUME = IElementType("assume", QuintLanguage.INSTANCE)
    @JvmField val TYPE = IElementType("type", QuintLanguage.INSTANCE)
    @JvmField val VAL = IElementType("val", QuintLanguage.INSTANCE)
    @JvmField val DEF = IElementType("def", QuintLanguage.INSTANCE)
    @JvmField val PURE = IElementType("pure", QuintLanguage.INSTANCE)
    @JvmField val ACTION = IElementType("action", QuintLanguage.INSTANCE)
    @JvmField val RUN = IElementType("run", QuintLanguage.INSTANCE)
    @JvmField val TEMPORAL = IElementType("temporal", QuintLanguage.INSTANCE)
    @JvmField val NONDET = IElementType("nondet", QuintLanguage.INSTANCE)
    @JvmField val IF = IElementType("if", QuintLanguage.INSTANCE)
    @JvmField val ELSE = IElementType("else", QuintLanguage.INSTANCE)
    @JvmField val MATCH = IElementType("match", QuintLanguage.INSTANCE)
    @JvmField val AND = IElementType("and", QuintLanguage.INSTANCE)
    @JvmField val OR = IElementType("or", QuintLanguage.INSTANCE)
    @JvmField val ALL = IElementType("all", QuintLanguage.INSTANCE)
    @JvmField val ANY = IElementType("any", QuintLanguage.INSTANCE)
    @JvmField val IFF = IElementType("iff", QuintLanguage.INSTANCE)
    @JvmField val IMPLIES = IElementType("implies", QuintLanguage.INSTANCE)
    @JvmField val SET = IElementType("Set", QuintLanguage.INSTANCE)
    @JvmField val LIST = IElementType("List", QuintLanguage.INSTANCE)

    // Operators
    @JvmField val PLUS = IElementType("+", QuintLanguage.INSTANCE)
    @JvmField val MINUS = IElementType("-", QuintLanguage.INSTANCE)
    @JvmField val MUL = IElementType("*", QuintLanguage.INSTANCE)
    @JvmField val DIV = IElementType("/", QuintLanguage.INSTANCE)
    @JvmField val MOD = IElementType("%", QuintLanguage.INSTANCE)
    @JvmField val POW = IElementType("^", QuintLanguage.INSTANCE)
    @JvmField val GT = IElementType(">", QuintLanguage.INSTANCE)
    @JvmField val LT = IElementType("<", QuintLanguage.INSTANCE)
    @JvmField val GE = IElementType(">=", QuintLanguage.INSTANCE)
    @JvmField val LE = IElementType("<=", QuintLanguage.INSTANCE)
    @JvmField val NE = IElementType("!=", QuintLanguage.INSTANCE)
    @JvmField val EQ = IElementType("==", QuintLanguage.INSTANCE)
    @JvmField val ASGN = IElementType("=", QuintLanguage.INSTANCE)
    @JvmField val ARROW = IElementType("->", QuintLanguage.INSTANCE)
    @JvmField val FAT_ARROW = IElementType("=>", QuintLanguage.INSTANCE)
    @JvmField val LPAREN = IElementType("(", QuintLanguage.INSTANCE)
    @JvmField val RPAREN = IElementType(")", QuintLanguage.INSTANCE)
    @JvmField val LBRACE = IElementType("{", QuintLanguage.INSTANCE)
    @JvmField val RBRACE = IElementType("}", QuintLanguage.INSTANCE)
    @JvmField val LBRACKET = IElementType("[", QuintLanguage.INSTANCE)
    @JvmField val RBRACKET = IElementType("]", QuintLanguage.INSTANCE)
    @JvmField val COMMA = IElementType(",", QuintLanguage.INSTANCE)
    @JvmField val COLON = IElementType(":", QuintLanguage.INSTANCE)
    @JvmField val DOT = IElementType(".", QuintLanguage.INSTANCE)
    @JvmField val SEMICOLON = IElementType(";", QuintLanguage.INSTANCE)
    @JvmField val PIPE = IElementType("|", QuintLanguage.INSTANCE)
    @JvmField val PRIME = IElementType("'", QuintLanguage.INSTANCE)
    @JvmField val COLONCOLON = IElementType("::", QuintLanguage.INSTANCE)
    @JvmField val SPREAD = IElementType("...", QuintLanguage.INSTANCE)
    @JvmField val UNDERSCORE = IElementType("_", QuintLanguage.INSTANCE)
    @JvmField val HASHBANG = IElementType("HASHBANG_LINE", QuintLanguage.INSTANCE)

    // Identifiers
    @JvmField val LOW_ID = IElementType("LOW_ID", QuintLanguage.INSTANCE)
    @JvmField val CAP_ID = IElementType("CAP_ID", QuintLanguage.INSTANCE)

    // Special
    @JvmField val BAD_CHARACTER = IElementType("BAD_CHARACTER", QuintLanguage.INSTANCE)

    // Token sets
    @JvmField val KEYWORDS = TokenSet.create(
        MODULE, IMPORT, EXPORT, FROM, AS, CONST, VAR, ASSUME, TYPE,
        VAL, DEF, PURE, ACTION, RUN, TEMPORAL, NONDET,
        IF, ELSE, MATCH, AND, OR, ALL, ANY, IFF, IMPLIES, SET, LIST
    )
    @JvmField val COMMENTS = TokenSet.create(COMMENT, LINE_COMMENT, DOCCOMMENT)
    @JvmField val STRINGS = TokenSet.create(STRING)
    @JvmField val BRACES = TokenSet.create(LBRACE, RBRACE)
    @JvmField val BRACKETS = TokenSet.create(LBRACKET, RBRACKET)
    @JvmField val PARENS = TokenSet.create(LPAREN, RPAREN)
    @JvmField val OPERATORS = TokenSet.create(
        PLUS, MINUS, MUL, DIV, MOD, POW, GT, LT, GE, LE, NE, EQ,
        ASGN, ARROW, FAT_ARROW
    )
    @JvmField val TYPE_KEYWORDS = TokenSet.create(SET, LIST)
    @JvmField val QUALIFIERS = TokenSet.create(VAL, DEF, PURE, ACTION, RUN, TEMPORAL, NONDET)
}
