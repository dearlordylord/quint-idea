/*
 * Copyright (c) 2016 by Sam Harwell, The ANTLR Project contributors.
 * Licensed under the BSD 2-Clause License. See vendor/LICENSE-antlr4-intellij-adaptor.txt.
 */
package org.antlr.intellij.adaptor.lexer;

import com.intellij.lang.Language;
import com.intellij.psi.tree.IElementType;
import org.antlr.v4.runtime.Token;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

/**
 * Represents an ANTLR token type as an IntelliJ {@link IElementType}.
 */
public class TokenIElementType extends IElementType {
    private final int antlrTokenType;

    public TokenIElementType(int antlrTokenType, @NotNull @NonNls String debugName, @NotNull Language language) {
        super(debugName, language);
        this.antlrTokenType = antlrTokenType;
    }

    /** Get the ANTLR token type represented by this element type. */
    public int getANTLRTokenType() {
        return antlrTokenType;
    }

    @Override
    public String toString() {
        return TokenIElementType.class.getSimpleName() + "." + super.toString();
    }
}
