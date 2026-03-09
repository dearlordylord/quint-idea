package com.dearlordylord.quint.idea.editor

import com.dearlordylord.quint.idea.QuintTokenTypes
import com.intellij.lang.BracePair
import com.intellij.lang.PairedBraceMatcher
import com.intellij.psi.PsiFile
import com.intellij.psi.tree.IElementType

class QuintBraceMatcher : PairedBraceMatcher {

    companion object {
        private val PAIRS = arrayOf(
            BracePair(QuintTokenTypes.LBRACE, QuintTokenTypes.RBRACE, true),
            BracePair(QuintTokenTypes.LBRACKET, QuintTokenTypes.RBRACKET, false),
            BracePair(QuintTokenTypes.LPAREN, QuintTokenTypes.RPAREN, false),
        )
    }

    override fun getPairs(): Array<BracePair> = PAIRS
    override fun isPairedBracesAllowedBeforeType(lbraceType: IElementType, contextType: IElementType?): Boolean = true
    override fun getCodeConstructStart(file: PsiFile?, openingBraceOffset: Int): Int = openingBraceOffset
}
