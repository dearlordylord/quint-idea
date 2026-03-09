/*
 * Copyright (c) 2016 by Sam Harwell, The ANTLR Project contributors.
 * Licensed under the BSD 2-Clause License. See vendor/LICENSE-antlr4-intellij-adaptor.txt.
 */
package org.antlr.intellij.adaptor.lexer;

import com.intellij.lang.Language;
import com.intellij.lexer.LexerBase;
import com.intellij.psi.tree.IElementType;
import org.antlr.v4.runtime.*;

import java.util.List;

/**
 * Wraps an ANTLR4 {@link Lexer} for use with IntelliJ's PSI/editor system.
 * <p>
 * IntelliJ calls {@link #start(CharSequence, int, int, int)} to initialize
 * the lexer, then repeatedly calls {@link #advance()} and inspects
 * {@link #getTokenType()}, {@link #getTokenStart()}, {@link #getTokenEnd()}.
 */
public class ANTLRLexerAdaptor extends LexerBase {
    private final Language language;
    private final Lexer lexer;
    private final List<TokenIElementType> tokenElementTypes;

    private CharSequence buffer;
    private int startOffset;
    private int endOffset;

    private Token currentToken;
    private int tokenType;

    public ANTLRLexerAdaptor(Language language, Lexer lexer) {
        this.language = language;
        this.lexer = lexer;
        this.tokenElementTypes = PSIElementTypeFactory.getTokenIElementTypes(language);
    }

    @Override
    public void start(CharSequence buffer, int startOffset, int endOffset, int initialState) {
        this.buffer = buffer;
        this.startOffset = startOffset;
        this.endOffset = endOffset;

        CharSequence input = buffer.subSequence(startOffset, endOffset);
        CharStream charStream = CharStreams.fromString(input.toString());
        lexer.setInputStream(charStream);

        // Restore lexer state from initialState if needed.
        // For single-mode lexers, initialState is always 0.
        lexer._mode = initialState;
        lexer._modeStack.clear();

        advance();
    }

    @Override
    public int getState() {
        // IntelliJ uses this to check if the lexer state changed.
        // For a single-mode lexer, mode is always 0.
        return lexer._mode;
    }

    @Override
    public IElementType getTokenType() {
        if (currentToken == null || currentToken.getType() == Token.EOF) {
            return null;
        }
        int type = currentToken.getType();
        if (type >= 0 && type < tokenElementTypes.size()) {
            return tokenElementTypes.get(type);
        }
        return null;
    }

    @Override
    public int getTokenStart() {
        if (currentToken == null) return startOffset;
        return startOffset + currentToken.getStartIndex();
    }

    @Override
    public int getTokenEnd() {
        if (currentToken == null) return startOffset;
        return startOffset + currentToken.getStopIndex() + 1;
    }

    @Override
    public void advance() {
        currentToken = lexer.nextToken();
    }

    @Override
    public CharSequence getBufferSequence() {
        return buffer;
    }

    @Override
    public int getBufferEnd() {
        return endOffset;
    }
}
