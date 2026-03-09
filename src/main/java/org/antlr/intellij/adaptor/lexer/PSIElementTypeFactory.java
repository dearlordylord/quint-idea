/*
 * Copyright (c) 2016 by Sam Harwell, The ANTLR Project contributors.
 * Licensed under the BSD 2-Clause License. See vendor/LICENSE-antlr4-intellij-adaptor.txt.
 */
package org.antlr.intellij.adaptor.lexer;

import com.intellij.lang.Language;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.tree.TokenSet;
import org.antlr.v4.runtime.Token;

import java.util.*;

/**
 * Factory for creating and registering {@link IElementType} instances that correspond
 * to ANTLR token types and parser rule indices.
 *
 * Call {@link #defineLanguageIElementTypes(Language, String[], String[])} once during
 * plugin initialization (e.g. in a companion object init block), then use
 * {@link #getTokenIElementTypes(Language)} and {@link #getRuleIElementTypes(Language)}
 * to retrieve the registered types.
 */
public class PSIElementTypeFactory {
    private static final Map<Language, List<TokenIElementType>> tokenIElementTypesMap = new HashMap<>();
    private static final Map<Language, List<RuleIElementType>> ruleIElementTypesMap = new HashMap<>();

    private PSIElementTypeFactory() {}

    /**
     * Define and register IElementType instances for all ANTLR tokens and rules
     * of the given language.
     *
     * @param language   the IntelliJ Language
     * @param tokenNames the array of token names from the ANTLR vocabulary
     *                   (use {@code parser.getTokenNames()} or {@code lexer.getTokenNames()})
     * @param ruleNames  the array of parser rule names
     *                   (use {@code parser.getRuleNames()})
     */
    public static void defineLanguageIElementTypes(Language language, String[] tokenNames, String[] ruleNames) {
        List<TokenIElementType> tokenTypes = new ArrayList<>();
        for (int i = 0; i < tokenNames.length; i++) {
            String name = tokenNames[i];
            if (name == null || name.isEmpty()) {
                name = "TOKEN_" + i;
            }
            tokenTypes.add(new TokenIElementType(i, name, language));
        }
        tokenIElementTypesMap.put(language, Collections.unmodifiableList(tokenTypes));

        List<RuleIElementType> ruleTypes = new ArrayList<>();
        for (int i = 0; i < ruleNames.length; i++) {
            ruleTypes.add(new RuleIElementType(i, ruleNames[i], language));
        }
        ruleIElementTypesMap.put(language, Collections.unmodifiableList(ruleTypes));
    }

    /** Get the list of token IElementTypes for the given language. */
    public static List<TokenIElementType> getTokenIElementTypes(Language language) {
        List<TokenIElementType> result = tokenIElementTypesMap.get(language);
        if (result == null) {
            throw new IllegalStateException("IElementTypes not defined for language: " + language.getID());
        }
        return result;
    }

    /** Get the list of rule IElementTypes for the given language. */
    public static List<RuleIElementType> getRuleIElementTypes(Language language) {
        List<RuleIElementType> result = ruleIElementTypesMap.get(language);
        if (result == null) {
            throw new IllegalStateException("IElementTypes not defined for language: " + language.getID());
        }
        return result;
    }

    /**
     * Create a {@link TokenSet} from the given ANTLR token type indices,
     * using the registered token IElementTypes for the language.
     */
    public static TokenSet createTokenSet(Language language, int... tokenTypes) {
        List<TokenIElementType> types = getTokenIElementTypes(language);
        IElementType[] elements = new IElementType[tokenTypes.length];
        for (int i = 0; i < tokenTypes.length; i++) {
            int tt = tokenTypes[i];
            if (tt >= 0 && tt < types.size()) {
                elements[i] = types.get(tt);
            }
        }
        return TokenSet.create(elements);
    }
}
