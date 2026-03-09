package com.dearlordylord.quint.idea.parser

import com.dearlordylord.quint.idea.QuintLanguage
import com.dearlordylord.quint.idea.psi.QuintFile
import com.intellij.lang.ASTNode
import com.intellij.lang.ParserDefinition
import com.intellij.lang.PsiParser
import com.intellij.lexer.Lexer
import com.intellij.openapi.project.Project
import com.intellij.psi.FileViewProvider
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.tree.IFileElementType
import com.intellij.psi.tree.TokenSet
import org.antlr.intellij.adaptor.lexer.ANTLRLexerAdaptor
import org.antlr.intellij.adaptor.lexer.PSIElementTypeFactory
import org.antlr.intellij.adaptor.psi.ANTLRPsiNode

class QuintParserDefinition : ParserDefinition {
    companion object {
        val FILE = IFileElementType(QuintLanguage.INSTANCE)

        init {
            val vocab = QuintParser.VOCABULARY
            val tokenNames = Array(vocab.maxTokenType + 1) { i ->
                vocab.getSymbolicName(i) ?: vocab.getLiteralName(i) ?: "TOKEN_$i"
            }
            PSIElementTypeFactory.defineLanguageIElementTypes(
                QuintLanguage.INSTANCE,
                tokenNames,
                QuintParser.ruleNames
            )
        }

        val TOKEN_ELEMENT_TYPES = PSIElementTypeFactory.getTokenIElementTypes(QuintLanguage.INSTANCE)
        val RULE_ELEMENT_TYPES = PSIElementTypeFactory.getRuleIElementTypes(QuintLanguage.INSTANCE)

        val COMMENTS: TokenSet = PSIElementTypeFactory.createTokenSet(
            QuintLanguage.INSTANCE,
            QuintLexer.LINE_COMMENT,
            QuintLexer.COMMENT
        )

        val WHITESPACE: TokenSet = PSIElementTypeFactory.createTokenSet(
            QuintLanguage.INSTANCE,
            QuintLexer.WS
        )

        val STRINGS: TokenSet = PSIElementTypeFactory.createTokenSet(
            QuintLanguage.INSTANCE,
            QuintLexer.STRING
        )
    }

    override fun createLexer(project: Project?): Lexer =
        ANTLRLexerAdaptor(QuintLanguage.INSTANCE, QuintLexer(null as org.antlr.v4.runtime.CharStream?))

    override fun createParser(project: Project?): PsiParser = QuintParserAdaptor()

    override fun getFileNodeType(): IFileElementType = FILE

    override fun getCommentTokens(): TokenSet = COMMENTS

    override fun getWhitespaceTokens(): TokenSet = WHITESPACE

    override fun getStringLiteralElements(): TokenSet = STRINGS

    override fun createElement(node: ASTNode): PsiElement = ANTLRPsiNode(node)

    override fun createFile(viewProvider: FileViewProvider): PsiFile = QuintFile(viewProvider)
}
