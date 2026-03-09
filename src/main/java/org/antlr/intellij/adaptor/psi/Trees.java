/*
 * Copyright (c) 2016 by Sam Harwell, The ANTLR Project contributors.
 * Licensed under the BSD 2-Clause License. See vendor/LICENSE-antlr4-intellij-adaptor.txt.
 */
package org.antlr.intellij.adaptor.psi;

import com.intellij.psi.PsiElement;
import com.intellij.psi.tree.IElementType;
import org.antlr.intellij.adaptor.lexer.RuleIElementType;
import org.antlr.intellij.adaptor.lexer.TokenIElementType;

import java.util.ArrayList;
import java.util.List;

/**
 * Utility methods for navigating PSI trees created from ANTLR grammars.
 */
public class Trees {

    private Trees() {}

    /**
     * Find all PSI children of the given element whose element type matches
     * the specified rule element type.
     */
    public static List<PsiElement> getChildren(PsiElement parent, RuleIElementType ruleType) {
        List<PsiElement> result = new ArrayList<>();
        for (PsiElement child = parent.getFirstChild(); child != null; child = child.getNextSibling()) {
            if (child.getNode() != null && child.getNode().getElementType() == ruleType) {
                result.add(child);
            }
        }
        return result;
    }

    /**
     * Find all PSI children of the given element whose element type matches
     * the specified token element type.
     */
    public static List<PsiElement> getChildren(PsiElement parent, TokenIElementType tokenType) {
        List<PsiElement> result = new ArrayList<>();
        for (PsiElement child = parent.getFirstChild(); child != null; child = child.getNextSibling()) {
            if (child.getNode() != null && child.getNode().getElementType() == tokenType) {
                result.add(child);
            }
        }
        return result;
    }

    /**
     * Find all descendant PSI elements with the specified element type.
     */
    public static List<PsiElement> findAllNodes(PsiElement root, IElementType type) {
        List<PsiElement> result = new ArrayList<>();
        findAllNodesRecursive(root, type, result);
        return result;
    }

    private static void findAllNodesRecursive(PsiElement element, IElementType type, List<PsiElement> result) {
        if (element.getNode() != null && element.getNode().getElementType() == type) {
            result.add(element);
        }
        for (PsiElement child = element.getFirstChild(); child != null; child = child.getNextSibling()) {
            findAllNodesRecursive(child, type, result);
        }
    }

    /**
     * Check if a PSI element represents the given ANTLR rule.
     */
    public static boolean isRuleNode(PsiElement element, int ruleIndex) {
        if (element == null || element.getNode() == null) return false;
        IElementType type = element.getNode().getElementType();
        return type instanceof RuleIElementType && ((RuleIElementType) type).getRuleIndex() == ruleIndex;
    }
}
