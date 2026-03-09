/*
 * Copyright (c) 2016 by Sam Harwell, The ANTLR Project contributors.
 * Licensed under the BSD 2-Clause License. See vendor/LICENSE-antlr4-intellij-adaptor.txt.
 */
package org.antlr.intellij.adaptor.lexer;

import org.antlr.v4.runtime.Lexer;
import org.antlr.v4.runtime.misc.IntegerStack;

/**
 * Captures ANTLR lexer state (current mode and mode stack) so that IntelliJ
 * can do incremental re-lexing. Two lexer states are equal if and only if
 * the mode and mode stack are identical.
 */
public class ANTLRLexerState {
    private final int mode;
    private final int[] modeStack;

    public ANTLRLexerState(int mode, int[] modeStack) {
        this.mode = mode;
        this.modeStack = modeStack != null ? modeStack.clone() : new int[0];
    }

    /** Create a state from the current state of an ANTLR lexer. */
    public static ANTLRLexerState fromLexer(Lexer lexer) {
        int mode = lexer._mode;
        IntegerStack stack = lexer._modeStack;
        int[] modeStack = stack != null && !stack.isEmpty() ? stack.toArray() : new int[0];
        return new ANTLRLexerState(mode, modeStack);
    }

    /** Apply this state to the given ANTLR lexer. */
    public void apply(Lexer lexer) {
        lexer._mode = mode;
        lexer._modeStack.clear();
        if (modeStack.length > 0) {
            for (int m : modeStack) {
                lexer._modeStack.push(m);
            }
        }
    }

    public int getMode() {
        return mode;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ANTLRLexerState)) return false;
        ANTLRLexerState other = (ANTLRLexerState) o;
        if (mode != other.mode) return false;
        if (modeStack.length != other.modeStack.length) return false;
        for (int i = 0; i < modeStack.length; i++) {
            if (modeStack[i] != other.modeStack[i]) return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int result = mode;
        for (int m : modeStack) {
            result = result * 31 + m;
        }
        return result;
    }
}
