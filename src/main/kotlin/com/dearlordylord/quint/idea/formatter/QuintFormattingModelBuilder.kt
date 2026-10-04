package com.dearlordylord.quint.idea.formatter

import com.dearlordylord.quint.idea.QuintLanguage
import com.dearlordylord.quint.idea.parser.QuintParserTokenTypes
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
        return SpacingBuilder(settings, QuintLanguage.INSTANCE)
            .after(QuintParserTokenTypes.COMMA).spaceIf(true)
            .before(QuintParserTokenTypes.COMMA).spaceIf(false)
            .before(QuintBlock.LBRACE).spaceIf(true)
            .after(QuintBlock.LPAREN).spaceIf(false)
            .before(QuintBlock.RPAREN).spaceIf(false)
            .after(QuintBlock.LBRACKET).spaceIf(false)
            .before(QuintBlock.RBRACKET).spaceIf(false)
            .around(QuintParserTokenTypes.ASGN).spaceIf(true)
            .around(QuintParserTokenTypes.EQ).spaceIf(true)
            .around(QuintParserTokenTypes.NE).spaceIf(true)
            .around(QuintParserTokenTypes.GE).spaceIf(true)
            .around(QuintParserTokenTypes.LE).spaceIf(true)
            .around(QuintParserTokenTypes.FAT_ARROW).spaceIf(true)
            .around(QuintParserTokenTypes.ARROW).spaceIf(true)
            .before(QuintParserTokenTypes.COLON).spaceIf(false)
            .after(QuintParserTokenTypes.COLON).spaceIf(true)
            .around(QuintParserTokenTypes.PLUS).spaceIf(true)
            .around(QuintParserTokenTypes.MINUS).spaceIf(true)
            .around(QuintParserTokenTypes.MUL).spaceIf(true)
            .around(QuintParserTokenTypes.DIV).spaceIf(true)
            .around(QuintParserTokenTypes.MOD).spaceIf(true)
            .around(QuintParserTokenTypes.GT).spaceIf(true)
            .around(QuintParserTokenTypes.LT).spaceIf(true)
    }
}
