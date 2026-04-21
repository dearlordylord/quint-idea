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
import org.antlr.v4.runtime.atn.PredictionMode;
import org.antlr.v4.runtime.misc.ParseCancellationException;
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

        ParseTree parseTree = runAntlrParse(text, root);
        if (parseTree == null) {
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
     * Run the ANTLR parse. Attempts SLL prediction mode first (much faster, cheap closure)
     * and falls back to full LL prediction on ambiguity. Returns null if both modes fail.
     */
    private ParseTree runAntlrParse(String text, IElementType root) {
        try {
            return doParse(text, root, PredictionMode.SLL, true);
        } catch (ParseCancellationException ignored) {
            // SLL couldn't decide — retry with full LL on a fresh lexer/parser.
            try {
                return doParse(text, root, PredictionMode.LL, false);
            } catch (Exception ignored2) {
                return null;
            }
        } catch (Exception ignored) {
            return null;
        }
    }

    private ParseTree doParse(String text, IElementType root, PredictionMode mode, boolean bailOnError) {
        CharStream charStream = CharStreams.fromString(text);
        Lexer antlrLexer = createANTLRLexer(charStream);
        CommonTokenStream tokenStream = new CommonTokenStream(antlrLexer);
        Parser antlrParser = createANTLRParser(tokenStream);
        antlrParser.removeErrorListeners();
        antlrParser.getInterpreter().setPredictionMode(mode);
        if (bailOnError) {
            antlrParser.setErrorHandler(new BailErrorStrategy());
        }
        return parse(antlrParser, root);
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
