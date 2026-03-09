/*
 * Copyright (c) 2016 by Sam Harwell, The ANTLR Project contributors.
 * Licensed under the BSD 2-Clause License. See vendor/LICENSE-antlr4-intellij-adaptor.txt.
 */
package org.antlr.intellij.adaptor.lexer;

import com.intellij.lang.Language;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

/**
 * Represents an ANTLR parser rule as an IntelliJ {@link IElementType}.
 */
public class RuleIElementType extends IElementType {
    private final int ruleIndex;

    public RuleIElementType(int ruleIndex, @NotNull @NonNls String debugName, @NotNull Language language) {
        super(debugName, language);
        this.ruleIndex = ruleIndex;
    }

    /** Get the ANTLR rule index represented by this element type. */
    public int getRuleIndex() {
        return ruleIndex;
    }

    @Override
    public String toString() {
        return RuleIElementType.class.getSimpleName() + "." + super.toString();
    }
}
