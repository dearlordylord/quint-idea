package com.dearlordylord.quint.idea.references

import com.dearlordylord.quint.idea.QuintLanguage
import com.dearlordylord.quint.idea.parser.QuintLexer
import com.dearlordylord.quint.idea.psi.QuintNamedElement
import com.dearlordylord.quint.idea.psi.QuintPsiUtils
import com.intellij.lang.cacheBuilder.DefaultWordsScanner
import com.intellij.lang.cacheBuilder.WordsScanner
import com.intellij.lang.findUsages.FindUsagesProvider
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiNamedElement
import org.antlr.intellij.adaptor.lexer.ANTLRLexerAdaptor
import org.antlr.intellij.adaptor.lexer.PSIElementTypeFactory

class QuintFindUsagesProvider : FindUsagesProvider {

    override fun getWordsScanner(): WordsScanner {
        return DefaultWordsScanner(
            ANTLRLexerAdaptor(QuintLanguage.INSTANCE, QuintLexer(null as org.antlr.v4.runtime.CharStream?)),
            PSIElementTypeFactory.createTokenSet(QuintLanguage.INSTANCE, QuintLexer.LOW_ID, QuintLexer.CAP_ID),
            PSIElementTypeFactory.createTokenSet(QuintLanguage.INSTANCE, QuintLexer.LINE_COMMENT, QuintLexer.COMMENT),
            PSIElementTypeFactory.createTokenSet(QuintLanguage.INSTANCE, QuintLexer.STRING)
        )
    }

    override fun canFindUsagesFor(psiElement: PsiElement): Boolean = psiElement is QuintNamedElement

    override fun getHelpId(psiElement: PsiElement): String? = null

    override fun getType(element: PsiElement): String =
        QuintPsiUtils.getDeclarationQualifier(element) ?: "symbol"

    override fun getDescriptiveName(element: PsiElement): String =
        (element as? PsiNamedElement)?.name ?: ""

    override fun getNodeText(element: PsiElement, useFullName: Boolean): String =
        (element as? PsiNamedElement)?.name ?: ""
}
