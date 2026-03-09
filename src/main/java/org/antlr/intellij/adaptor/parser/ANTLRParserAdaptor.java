/*
 * Copyright (c) 2016 by Sam Harwell, The ANTLR Project contributors.
 * Licensed under the BSD 2-Clause License. See vendor/LICENSE-antlr4-intellij-adaptor.txt.
 */
package org.antlr.intellij.adaptor.parser;

import com.intellij.lang.ASTNode;
import com.intellij.lang.Language;
import com.intellij.lang.PsiBuilder;
import com.intellij.lang.PsiParser;
import com.intellij.psi.tree.IElementType;
import org.antlr.intellij.adaptor.lexer.PSIElementTypeFactory;
import org.antlr.intellij.adaptor.lexer.RuleIElementType;
import org.antlr.intellij.adaptor.lexer.TokenIElementType;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.tree.*;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Bridges an ANTLR4 {@link Parser} to IntelliJ's {@link PsiParser} interface.
 *
 * <p>Approach:
 * <ol>
 *   <li>Parse the entire text with ANTLR independently to get a parse tree</li>
 *   <li>Walk the ANTLR parse tree and build IntelliJ PSI using PsiBuilder markers</li>
 * </ol></p>
 *
 * <p>Subclasses must implement {@link #parse(Parser, IElementType)} to invoke
 * the appropriate entry rule on the ANTLR parser.</p>
 */
public abstract class ANTLRParserAdaptor implements PsiParser {
    private final Language language;
    private final Parser parserPrototype;

    public ANTLRParserAdaptor(Language language, Parser parser) {
        this.language = language;
        this.parserPrototype = parser;
    }

    protected Language getLanguage() {
        return language;
    }

    /**
     * Subclasses implement this to invoke the correct entry rule.
     * E.g., {@code return ((MyParser)parser).compilationUnit();}
     */
    protected abstract ParseTree parse(Parser parser, IElementType root);

    @Override
    @NotNull
    public ASTNode parse(@NotNull IElementType root, @NotNull PsiBuilder builder) {
        PsiBuilder.Marker rootMarker = builder.mark();

        // Phase 1: Parse with ANTLR independently to get the parse tree structure.
        String text = builder.getOriginalText().toString();
        CharStream charStream = CharStreams.fromString(text);

        // Create a fresh lexer for ANTLR parsing
        Lexer antlrLexer = createANTLRLexer(charStream);
        CommonTokenStream tokenStream = new CommonTokenStream(antlrLexer);

        // Create a fresh parser
        Parser antlrParser = createANTLRParser(tokenStream);
        antlrParser.removeErrorListeners();

        ParseTree parseTree;
        try {
            parseTree = parse(antlrParser, root);
        } catch (Exception e) {
            // On failure, consume everything flat
            while (!builder.eof()) {
                builder.advanceLexer();
            }
            rootMarker.done(root);
            return builder.getTreeBuilt();
        }

        // Phase 2: Walk the ANTLR parse tree and build PSI using PsiBuilder.
        // We flatten the tree into the PsiBuilder by walking and creating markers.
        buildPSI(parseTree, builder);

        // Consume any remaining tokens (trailing whitespace, etc.)
        while (!builder.eof()) {
            builder.advanceLexer();
        }

        rootMarker.done(root);
        return builder.getTreeBuilt();
    }

    /**
     * Create an ANTLR lexer for the given input. Override if needed.
     * Default implementation creates a new instance of the lexer class from the parser's token stream.
     */
    protected Lexer createANTLRLexer(CharStream input) {
        // Get the lexer class from the parser's vocabulary
        // Subclasses should override this if the default doesn't work
        throw new UnsupportedOperationException("Subclass must override createANTLRLexer()");
    }

    /**
     * Create a fresh ANTLR parser for the given token stream. Override if needed.
     */
    protected Parser createANTLRParser(TokenStream tokenStream) {
        throw new UnsupportedOperationException("Subclass must override createANTLRParser()");
    }

    /**
     * Walk the ANTLR parse tree and create PsiBuilder markers.
     */
    private void buildPSI(ParseTree tree, PsiBuilder builder) {
        List<RuleIElementType> ruleTypes = PSIElementTypeFactory.getRuleIElementTypes(language);
        walkTree(tree, builder, ruleTypes);
    }

    private void walkTree(ParseTree tree, PsiBuilder builder, List<RuleIElementType> ruleTypes) {
        if (tree instanceof TerminalNode) {
            // Terminal: advance the PsiBuilder lexer to consume this token
            if (((TerminalNode) tree).getSymbol().getType() != Token.EOF) {
                builder.advanceLexer();
            }
        } else if (tree instanceof ParserRuleContext) {
            ParserRuleContext ctx = (ParserRuleContext) tree;
            PsiBuilder.Marker marker = builder.mark();

            // Recurse into children
            for (int i = 0; i < tree.getChildCount(); i++) {
                walkTree(tree.getChild(i), builder, ruleTypes);
            }

            int ruleIndex = ctx.getRuleIndex();
            if (ruleIndex >= 0 && ruleIndex < ruleTypes.size()) {
                marker.done(ruleTypes.get(ruleIndex));
            } else {
                marker.drop();
            }
        }
    }
}
