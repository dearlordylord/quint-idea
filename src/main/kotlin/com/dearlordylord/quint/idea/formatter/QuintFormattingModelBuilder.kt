package com.dearlordylord.quint.idea.formatter

import com.dearlordylord.quint.idea.QuintLanguage
import com.dearlordylord.quint.idea.parser.QuintLexer
import com.dearlordylord.quint.idea.parser.QuintParserDefinition
import com.intellij.formatting.*

class QuintFormattingModelBuilder : FormattingModelBuilder {
    override fun createModel(formattingContext: FormattingContext): FormattingModel {
        val settings = formattingContext.codeStyleSettings
        val spacingBuilder = createSpacingBuilder(settings)
        val rootBlock = QuintBlock(formattingContext.node, null, null, spacingBuilder)
        return FormattingModelProvider.createFormattingModelForPsiFile(
            formattingContext.containingFile, rootBlock, settings
        )
    }

    private fun createSpacingBuilder(settings: com.intellij.psi.codeStyle.CodeStyleSettings): SpacingBuilder {
        val tokenTypes = QuintParserDefinition.TOKEN_ELEMENT_TYPES
        val comma = tokenTypes[QuintLexer.T__7]        // ,
        val colon = tokenTypes[QuintLexer.T__4]        // :
        val eq = tokenTypes[QuintLexer.EQ]             // ==
        val ne = tokenTypes[QuintLexer.NE]             // !=
        val ge = tokenTypes[QuintLexer.GE]             // >=
        val le = tokenTypes[QuintLexer.LE]             // <=
        val asgn = tokenTypes[QuintLexer.ASGN]         // =
        val arrow = tokenTypes[QuintLexer.T__21]       // ->
        val fatArrow = tokenTypes[QuintLexer.T__22]    // =>
        val plus = tokenTypes[QuintLexer.PLUS]
        val minus = tokenTypes[QuintLexer.MINUS]
        val mul = tokenTypes[QuintLexer.MUL]
        val div = tokenTypes[QuintLexer.DIV]
        val mod = tokenTypes[QuintLexer.MOD]
        val gt = tokenTypes[QuintLexer.GT]
        val lt = tokenTypes[QuintLexer.LT]

        return SpacingBuilder(settings, QuintLanguage.INSTANCE)
            .after(comma).spaceIf(true)
            .before(comma).spaceIf(false)
            .before(QuintBlock.LBRACE).spaceIf(true)
            .after(QuintBlock.LPAREN).spaceIf(false)
            .before(QuintBlock.RPAREN).spaceIf(false)
            .after(QuintBlock.LBRACKET).spaceIf(false)
            .before(QuintBlock.RBRACKET).spaceIf(false)
            .around(asgn).spaceIf(true)
            .around(eq).spaceIf(true)
            .around(ne).spaceIf(true)
            .around(ge).spaceIf(true)
            .around(le).spaceIf(true)
            .around(fatArrow).spaceIf(true)
            .around(arrow).spaceIf(true)
            .around(colon).spaceIf(true)
            .around(plus).spaceIf(true)
            .around(minus).spaceIf(true)
            .around(mul).spaceIf(true)
            .around(div).spaceIf(true)
            .around(mod).spaceIf(true)
            .around(gt).spaceIf(true)
            .around(lt).spaceIf(true)
    }
}
