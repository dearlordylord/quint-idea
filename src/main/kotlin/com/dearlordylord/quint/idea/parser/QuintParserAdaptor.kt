package com.dearlordylord.quint.idea.parser

import com.dearlordylord.quint.idea.QuintLanguage
import com.intellij.psi.tree.IElementType
import org.antlr.intellij.adaptor.parser.ANTLRParserAdaptor
import org.antlr.v4.runtime.*
import org.antlr.v4.runtime.tree.ParseTree

class QuintParserAdaptor : ANTLRParserAdaptor(QuintLanguage.INSTANCE, QuintParser(null as TokenStream?)) {
    override fun parse(parser: Parser, root: IElementType): ParseTree {
        return (parser as QuintParser).modules()
    }

    override fun createANTLRLexer(input: CharStream): Lexer {
        return QuintLexer(input)
    }

    override fun createANTLRParser(tokenStream: TokenStream): Parser {
        return QuintParser(tokenStream)
    }
}
