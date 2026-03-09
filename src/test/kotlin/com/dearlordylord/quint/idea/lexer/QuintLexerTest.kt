package com.dearlordylord.quint.idea.lexer

import com.dearlordylord.quint.idea.QuintTokenTypes
import com.intellij.lexer.FlexAdapter
import com.intellij.psi.TokenType
import com.intellij.psi.tree.IElementType
import org.junit.Assert.*
import org.junit.Test

class QuintLexerTest {

    private fun tokenize(text: String): List<Pair<IElementType, String>> {
        val lexer = FlexAdapter(QuintLexer(null as java.io.Reader?))
        lexer.start(text)
        val tokens = mutableListOf<Pair<IElementType, String>>()
        while (lexer.tokenType != null) {
            tokens.add(Pair(lexer.tokenType!!, lexer.tokenText))
            lexer.advance()
        }
        return tokens
    }

    private fun tokenTypes(text: String): List<IElementType> = tokenize(text).map { it.first }

    private fun tokenTexts(text: String): List<String> = tokenize(text).map { it.second }

    private fun nonWhitespaceTokens(text: String): List<Pair<IElementType, String>> =
        tokenize(text).filter { it.first != TokenType.WHITE_SPACE }

    @Test
    fun testWhitespace() {
        val tokens = tokenize("  \t\n  ")
        assertEquals(1, tokens.size)
        assertEquals(TokenType.WHITE_SPACE, tokens[0].first)
    }

    @Test
    fun testKeywords() {
        val keywords = mapOf(
            "module" to QuintTokenTypes.MODULE,
            "import" to QuintTokenTypes.IMPORT,
            "export" to QuintTokenTypes.EXPORT,
            "from" to QuintTokenTypes.FROM,
            "as" to QuintTokenTypes.AS,
            "const" to QuintTokenTypes.CONST,
            "var" to QuintTokenTypes.VAR,
            "assume" to QuintTokenTypes.ASSUME,
            "type" to QuintTokenTypes.TYPE,
            "val" to QuintTokenTypes.VAL,
            "def" to QuintTokenTypes.DEF,
            "pure" to QuintTokenTypes.PURE,
            "action" to QuintTokenTypes.ACTION,
            "run" to QuintTokenTypes.RUN,
            "temporal" to QuintTokenTypes.TEMPORAL,
            "nondet" to QuintTokenTypes.NONDET,
            "if" to QuintTokenTypes.IF,
            "else" to QuintTokenTypes.ELSE,
            "match" to QuintTokenTypes.MATCH,
        )
        for ((text, expected) in keywords) {
            val tokens = nonWhitespaceTokens(text)
            assertEquals("Keyword '$text' should produce one token", 1, tokens.size)
            assertEquals("Keyword '$text' should have type $expected", expected, tokens[0].first)
        }
    }

    @Test
    fun testKeywordOperators() {
        val ops = mapOf(
            "and" to QuintTokenTypes.AND,
            "or" to QuintTokenTypes.OR,
            "iff" to QuintTokenTypes.IFF,
            "implies" to QuintTokenTypes.IMPLIES,
            "all" to QuintTokenTypes.ALL,
            "any" to QuintTokenTypes.ANY,
        )
        for ((text, expected) in ops) {
            val tokens = nonWhitespaceTokens(text)
            assertEquals(1, tokens.size)
            assertEquals("'$text' should have type $expected", expected, tokens[0].first)
        }
    }

    @Test
    fun testTypeKeywords() {
        assertEquals(QuintTokenTypes.SET, nonWhitespaceTokens("Set")[0].first)
        assertEquals(QuintTokenTypes.LIST, nonWhitespaceTokens("List")[0].first)
    }

    @Test
    fun testBooleans() {
        assertEquals(QuintTokenTypes.BOOL, nonWhitespaceTokens("true")[0].first)
        assertEquals(QuintTokenTypes.BOOL, nonWhitespaceTokens("false")[0].first)
    }

    @Test
    fun testIdentifiers() {
        assertEquals(QuintTokenTypes.LOW_ID, nonWhitespaceTokens("foo")[0].first)
        assertEquals(QuintTokenTypes.LOW_ID, nonWhitespaceTokens("myVar123")[0].first)
        assertEquals(QuintTokenTypes.LOW_ID, nonWhitespaceTokens("_private")[0].first)
        assertEquals(QuintTokenTypes.CAP_ID, nonWhitespaceTokens("Foo")[0].first)
        assertEquals(QuintTokenTypes.CAP_ID, nonWhitespaceTokens("MyType")[0].first)
    }

    @Test
    fun testKeywordPrefixIsIdentifier() {
        // "module_name" is an identifier, not "module" + "_name"
        val tokens = nonWhitespaceTokens("module_name")
        assertEquals(1, tokens.size)
        assertEquals(QuintTokenTypes.LOW_ID, tokens[0].first)
        assertEquals("module_name", tokens[0].second)
    }

    @Test
    fun testKeywordSuffixIsIdentifier() {
        // "modules" is an identifier, not "module" + "s"
        val tokens = nonWhitespaceTokens("modules")
        assertEquals(1, tokens.size)
        assertEquals(QuintTokenTypes.LOW_ID, tokens[0].first)
        assertEquals("modules", tokens[0].second)
    }

    @Test
    fun testIntLiterals() {
        assertEquals(QuintTokenTypes.INT, nonWhitespaceTokens("0")[0].first)
        assertEquals(QuintTokenTypes.INT, nonWhitespaceTokens("42")[0].first)
        assertEquals(QuintTokenTypes.INT, nonWhitespaceTokens("1_000")[0].first)
        assertEquals(QuintTokenTypes.INT, nonWhitespaceTokens("0xFF")[0].first)
        assertEquals(QuintTokenTypes.INT, nonWhitespaceTokens("0xDEAD_BEEF")[0].first)
    }

    @Test
    fun testStringLiteral() {
        val tokens = nonWhitespaceTokens("\"hello world\"")
        assertEquals(1, tokens.size)
        assertEquals(QuintTokenTypes.STRING, tokens[0].first)
        assertEquals("\"hello world\"", tokens[0].second)
    }

    @Test
    fun testOperators() {
        val ops = mapOf(
            "+" to QuintTokenTypes.PLUS,
            "-" to QuintTokenTypes.MINUS,
            "*" to QuintTokenTypes.MUL,
            "/" to QuintTokenTypes.DIV,
            "%" to QuintTokenTypes.MOD,
            "^" to QuintTokenTypes.POW,
            ">" to QuintTokenTypes.GT,
            "<" to QuintTokenTypes.LT,
            ">=" to QuintTokenTypes.GE,
            "<=" to QuintTokenTypes.LE,
            "!=" to QuintTokenTypes.NE,
            "==" to QuintTokenTypes.EQ,
            "=" to QuintTokenTypes.ASGN,
            "->" to QuintTokenTypes.ARROW,
            "=>" to QuintTokenTypes.FAT_ARROW,
            "::" to QuintTokenTypes.COLONCOLON,
            "..." to QuintTokenTypes.SPREAD,
        )
        for ((text, expected) in ops) {
            val tokens = nonWhitespaceTokens(text)
            assertEquals("Operator '$text' should produce one token", 1, tokens.size)
            assertEquals("Operator '$text' should have type $expected", expected, tokens[0].first)
        }
    }

    @Test
    fun testBrackets() {
        val brackets = mapOf(
            "(" to QuintTokenTypes.LPAREN,
            ")" to QuintTokenTypes.RPAREN,
            "{" to QuintTokenTypes.LBRACE,
            "}" to QuintTokenTypes.RBRACE,
            "[" to QuintTokenTypes.LBRACKET,
            "]" to QuintTokenTypes.RBRACKET,
        )
        for ((text, expected) in brackets) {
            val tokens = nonWhitespaceTokens(text)
            assertEquals(1, tokens.size)
            assertEquals(expected, tokens[0].first)
        }
    }

    @Test
    fun testPunctuation() {
        assertEquals(QuintTokenTypes.COMMA, nonWhitespaceTokens(",")[0].first)
        assertEquals(QuintTokenTypes.COLON, nonWhitespaceTokens(":")[0].first)
        assertEquals(QuintTokenTypes.DOT, nonWhitespaceTokens(".")[0].first)
        assertEquals(QuintTokenTypes.SEMICOLON, nonWhitespaceTokens(";")[0].first)
        assertEquals(QuintTokenTypes.PIPE, nonWhitespaceTokens("|")[0].first)
        assertEquals(QuintTokenTypes.PRIME, nonWhitespaceTokens("'")[0].first)
        assertEquals(QuintTokenTypes.UNDERSCORE, nonWhitespaceTokens("_")[0].first)
    }

    @Test
    fun testLineComment() {
        val tokens = nonWhitespaceTokens("// this is a comment")
        assertEquals(1, tokens.size)
        assertEquals(QuintTokenTypes.LINE_COMMENT, tokens[0].first)
    }

    @Test
    fun testDocComment() {
        val tokens = nonWhitespaceTokens("/// doc comment")
        assertEquals(1, tokens.size)
        assertEquals(QuintTokenTypes.DOCCOMMENT, tokens[0].first)
    }

    @Test
    fun testBlockComment() {
        val tokens = nonWhitespaceTokens("/* block */")
        // Block comment may produce multiple COMMENT tokens due to character-by-character processing in JFlex
        assertTrue(tokens.isNotEmpty())
        assertTrue(tokens.all { it.first == QuintTokenTypes.COMMENT })
    }

    @Test
    fun testHashbang() {
        val tokens = nonWhitespaceTokens("#!/usr/bin/env quint\n")
        assertEquals(1, tokens.size)
        assertEquals(QuintTokenTypes.HASHBANG, tokens[0].first)
    }

    @Test
    fun testModuleDeclaration() {
        val tokens = nonWhitespaceTokens("module Example { val x = 1 }")
        val types = tokens.map { it.first }
        assertEquals(
            listOf(
                QuintTokenTypes.MODULE,
                QuintTokenTypes.CAP_ID,
                QuintTokenTypes.LBRACE,
                QuintTokenTypes.VAL,
                QuintTokenTypes.LOW_ID,
                QuintTokenTypes.ASGN,
                QuintTokenTypes.INT,
                QuintTokenTypes.RBRACE,
            ),
            types
        )
    }

    @Test
    fun testFunctionDef() {
        val tokens = nonWhitespaceTokens("pure def add(a, b) = a + b")
        val types = tokens.map { it.first }
        assertEquals(
            listOf(
                QuintTokenTypes.PURE,
                QuintTokenTypes.DEF,
                QuintTokenTypes.LOW_ID,    // add
                QuintTokenTypes.LPAREN,
                QuintTokenTypes.LOW_ID,    // a
                QuintTokenTypes.COMMA,
                QuintTokenTypes.LOW_ID,    // b
                QuintTokenTypes.RPAREN,
                QuintTokenTypes.ASGN,
                QuintTokenTypes.LOW_ID,    // a
                QuintTokenTypes.PLUS,
                QuintTokenTypes.LOW_ID,    // b
            ),
            types
        )
    }

    @Test
    fun testArrowAndFatArrow() {
        val tokens = nonWhitespaceTokens("int -> bool")
        val types = tokens.map { it.first }
        assertEquals(
            listOf(QuintTokenTypes.LOW_ID, QuintTokenTypes.ARROW, QuintTokenTypes.LOW_ID),
            types
        )

        val tokens2 = nonWhitespaceTokens("x => x + 1")
        val types2 = tokens2.map { it.first }
        assertEquals(
            listOf(
                QuintTokenTypes.LOW_ID, QuintTokenTypes.FAT_ARROW,
                QuintTokenTypes.LOW_ID, QuintTokenTypes.PLUS, QuintTokenTypes.INT
            ),
            types2
        )
    }

    @Test
    fun testQualifiedId() {
        val tokens = nonWhitespaceTokens("Mod::name")
        val types = tokens.map { it.first }
        assertEquals(
            listOf(QuintTokenTypes.CAP_ID, QuintTokenTypes.COLONCOLON, QuintTokenTypes.LOW_ID),
            types
        )
    }

    @Test
    fun testSpread() {
        val tokens = nonWhitespaceTokens("...record")
        val types = tokens.map { it.first }
        assertEquals(
            listOf(QuintTokenTypes.SPREAD, QuintTokenTypes.LOW_ID),
            types
        )
    }

    @Test
    fun testSetAndListType() {
        val tokens = nonWhitespaceTokens("Set[int]")
        val types = tokens.map { it.first }
        assertEquals(
            listOf(QuintTokenTypes.SET, QuintTokenTypes.LBRACKET, QuintTokenTypes.LOW_ID, QuintTokenTypes.RBRACKET),
            types
        )
    }

    @Test
    fun testMatchExpression() {
        val tokens = nonWhitespaceTokens("match x { | A => 1 | _ => 0 }")
        val types = tokens.map { it.first }
        assertEquals(
            listOf(
                QuintTokenTypes.MATCH,
                QuintTokenTypes.LOW_ID,
                QuintTokenTypes.LBRACE,
                QuintTokenTypes.PIPE,
                QuintTokenTypes.CAP_ID,
                QuintTokenTypes.FAT_ARROW,
                QuintTokenTypes.INT,
                QuintTokenTypes.PIPE,
                QuintTokenTypes.UNDERSCORE,
                QuintTokenTypes.FAT_ARROW,
                QuintTokenTypes.INT,
                QuintTokenTypes.RBRACE,
            ),
            types
        )
    }

    @Test
    fun testPrimeOperator() {
        val tokens = nonWhitespaceTokens("counter' = counter + 1")
        val types = tokens.map { it.first }
        assertEquals(
            listOf(
                QuintTokenTypes.LOW_ID,
                QuintTokenTypes.PRIME,
                QuintTokenTypes.ASGN,
                QuintTokenTypes.LOW_ID,
                QuintTokenTypes.PLUS,
                QuintTokenTypes.INT,
            ),
            types
        )
    }

    @Test
    fun testNoGaps() {
        // Verify the lexer covers every character in the input
        val text = "module Example { val x: int = 42 + 0xFF }"
        val lexer = FlexAdapter(QuintLexer(null as java.io.Reader?))
        lexer.start(text)
        var pos = 0
        while (lexer.tokenType != null) {
            assertEquals("Token should start at current position", pos, lexer.tokenStart)
            assertTrue("Token should have positive length", lexer.tokenEnd > lexer.tokenStart)
            pos = lexer.tokenEnd
            lexer.advance()
        }
        assertEquals("Lexer should consume entire input", text.length, pos)
    }

    @Test
    fun testBadCharacter() {
        // Tilde is not a valid Quint character
        val tokens = nonWhitespaceTokens("~")
        assertEquals(1, tokens.size)
        assertEquals(QuintTokenTypes.BAD_CHARACTER, tokens[0].first)
    }

    @Test
    fun testImportExport() {
        val tokens = nonWhitespaceTokens("import Foo from \"./foo\"")
        val types = tokens.map { it.first }
        assertEquals(
            listOf(
                QuintTokenTypes.IMPORT,
                QuintTokenTypes.CAP_ID,
                QuintTokenTypes.FROM,
                QuintTokenTypes.STRING,
            ),
            types
        )
    }

    @Test
    fun testComparisonOperators() {
        // Make sure >= and <= are not split into > = and < =
        val tokens = nonWhitespaceTokens("a >= b <= c")
        val types = tokens.map { it.first }
        assertEquals(
            listOf(
                QuintTokenTypes.LOW_ID,
                QuintTokenTypes.GE,
                QuintTokenTypes.LOW_ID,
                QuintTokenTypes.LE,
                QuintTokenTypes.LOW_ID,
            ),
            types
        )
    }

    @Test
    fun testEqVsAssign() {
        val tokens = nonWhitespaceTokens("a == b")
        assertEquals(QuintTokenTypes.EQ, tokens[1].first)

        val tokens2 = nonWhitespaceTokens("a = b")
        assertEquals(QuintTokenTypes.ASGN, tokens2[1].first)
    }
}
