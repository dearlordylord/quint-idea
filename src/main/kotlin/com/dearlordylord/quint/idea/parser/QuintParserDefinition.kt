package com.dearlordylord.quint.idea.parser

import com.dearlordylord.quint.idea.QuintLanguage
import com.dearlordylord.quint.idea.QuintTokenTypes
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

class QuintParserDefinition : ParserDefinition {
    companion object {
        val FILE = IFileElementType(QuintLanguage.INSTANCE)
    }

    override fun createLexer(project: Project?): Lexer = TODO("stub — replaced by T2")
    override fun createParser(project: Project?): PsiParser = TODO("stub — replaced by T2")
    override fun getFileNodeType(): IFileElementType = FILE
    override fun getCommentTokens(): TokenSet = QuintTokenTypes.COMMENTS
    override fun getStringLiteralElements(): TokenSet = QuintTokenTypes.STRINGS
    override fun createElement(node: ASTNode): PsiElement = TODO("stub — replaced by T2")
    override fun createFile(viewProvider: FileViewProvider): PsiFile = QuintFile(viewProvider)
}
