/*
 * Copyright (c) 2016 by Sam Harwell, The ANTLR Project contributors.
 * Licensed under the BSD 2-Clause License. See vendor/LICENSE-antlr4-intellij-adaptor.txt.
 */
package org.antlr.intellij.adaptor.parser;

import com.intellij.lang.Language;
import com.intellij.lang.PsiBuilder;
import org.antlr.intellij.adaptor.lexer.PSIElementTypeFactory;
import org.antlr.intellij.adaptor.lexer.RuleIElementType;
import org.antlr.intellij.adaptor.lexer.TokenIElementType;
import org.antlr.v4.runtime.Parser;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.ErrorNode;
import org.antlr.v4.runtime.tree.ParseTreeListener;
import org.antlr.v4.runtime.tree.TerminalNode;

import java.util.*;

/**
 * Converts an ANTLR4 parse tree into IntelliJ's PSI tree structure by listening
 * to parse tree walk events and creating corresponding {@link PsiBuilder.Marker}
 * instances.
 *
 * <p>This is used internally by {@link ANTLRParserAdaptor} after ANTLR parsing
 * completes. The converter walks the parse tree and uses PsiBuilder markers to
 * reconstruct the tree structure for IntelliJ.</p>
 */
public class ANTLRParseTreeToPSIConverter implements ParseTreeListener {
    private final Language language;
    private final Parser parser;
    private final PsiBuilder builder;
    private final List<RuleIElementType> ruleElementTypes;
    private final List<TokenIElementType> tokenElementTypes;
    private final Deque<PsiBuilder.Marker> markers = new ArrayDeque<>();

    public ANTLRParseTreeToPSIConverter(Language language, Parser parser, PsiBuilder builder) {
        this.language = language;
        this.parser = parser;
        this.builder = builder;
        this.ruleElementTypes = PSIElementTypeFactory.getRuleIElementTypes(language);
        this.tokenElementTypes = PSIElementTypeFactory.getTokenIElementTypes(language);
    }

    protected Language getLanguage() { return language; }
    protected Parser getParser() { return parser; }
    protected PsiBuilder getBuilder() { return builder; }
    protected Deque<PsiBuilder.Marker> getMarkers() { return markers; }

    @Override
    public void enterEveryRule(ParserRuleContext ctx) {
        markers.push(builder.mark());
    }

    @Override
    public void exitEveryRule(ParserRuleContext ctx) {
        PsiBuilder.Marker marker = markers.pop();
        marker.done(ruleElementTypes.get(ctx.getRuleIndex()));
    }

    @Override
    public void visitTerminal(TerminalNode node) {
        // Consume tokens in the PsiBuilder to keep it in sync with ANTLR's token stream.
        // We need to advance past any whitespace/comment tokens that PsiBuilder auto-skips.
        builder.advanceLexer();
    }

    @Override
    public void visitErrorNode(ErrorNode node) {
        // Error nodes still consume a token position
        builder.advanceLexer();
    }
}
