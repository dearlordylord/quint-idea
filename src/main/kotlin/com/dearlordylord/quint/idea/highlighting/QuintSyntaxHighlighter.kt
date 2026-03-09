package com.dearlordylord.quint.idea.highlighting

import com.intellij.lexer.FlexAdapter
import com.intellij.lexer.Lexer
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.fileTypes.SyntaxHighlighterBase
import com.intellij.psi.tree.IElementType

class QuintSyntaxHighlighter : SyntaxHighlighterBase() {
    override fun getHighlightingLexer(): Lexer =
        FlexAdapter(com.dearlordylord.quint.idea.lexer.QuintLexer(null as java.io.Reader?))
    override fun getTokenHighlights(tokenType: IElementType): Array<TextAttributesKey> = emptyArray()
}
