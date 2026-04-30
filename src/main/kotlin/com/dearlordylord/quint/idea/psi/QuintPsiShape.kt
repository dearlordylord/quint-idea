package com.dearlordylord.quint.idea.psi

import com.dearlordylord.quint.idea.parser.QuintParser
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import org.antlr.intellij.adaptor.lexer.RuleIElementType
import org.antlr.intellij.adaptor.lexer.TokenIElementType

object QuintPsiShape {

    fun ruleIndex(element: PsiElement?): Int? {
        val type = element?.node?.elementType as? RuleIElementType ?: return null
        return type.ruleIndex
    }

    fun isRule(element: PsiElement?, ruleIndex: Int): Boolean =
        ruleIndex(element) == ruleIndex

    fun directChildrenOfRule(parent: PsiElement, ruleIndex: Int): List<PsiElement> {
        val result = mutableListOf<PsiElement>()
        var child = parent.firstChild
        while (child != null) {
            if (isRule(child, ruleIndex)) result.add(child)
            child = child.nextSibling
        }
        return result
    }

    fun firstDirectChildOfRule(parent: PsiElement, ruleIndex: Int): PsiElement? {
        var child = parent.firstChild
        while (child != null) {
            if (isRule(child, ruleIndex)) return child
            child = child.nextSibling
        }
        return null
    }

    fun firstDescendantOfRule(parent: PsiElement, ruleIndex: Int, maxDepth: Int = 5): PsiElement? {
        descendantsOfRule(parent, ruleIndex, maxDepth).firstOrNull()?.let { return it }
        return null
    }

    fun descendantsOfRule(parent: PsiElement, ruleIndex: Int, maxDepth: Int = 5): List<PsiElement> {
        val result = mutableListOf<PsiElement>()
        collectDescendantsOfRule(parent, ruleIndex, maxDepth, result)
        return result
    }

    private fun collectDescendantsOfRule(
        element: PsiElement,
        ruleIndex: Int,
        maxDepth: Int,
        result: MutableList<PsiElement>
    ) {
        if (maxDepth <= 0) return
        var child = element.firstChild
        while (child != null) {
            if (isRule(child, ruleIndex)) {
                result.add(child)
            } else {
                collectDescendantsOfRule(child, ruleIndex, maxDepth - 1, result)
            }
            child = child.nextSibling
        }
    }

    fun isDotCallExpr(expr: PsiElement): Boolean {
        if (!isRule(expr, QuintParser.RULE_expr)) return false

        var hasDot = false
        var hasNameAfterDot = false
        var child = expr.firstChild
        while (child != null) {
            if (child.text == ".") hasDot = true
            if (isRule(child, QuintParser.RULE_nameAfterDot)) hasNameAfterDot = true
            if (hasDot && hasNameAfterDot) return true
            child = child.nextSibling
        }
        return false
    }

    fun enclosingDotCall(position: PsiElement, maxDepth: Int = 10): PsiElement? {
        var current = position.parent
        var depth = 0
        while (current != null && depth < maxDepth) {
            if (isDotCallExpr(current)) return current
            current = current.parent
            depth++
        }
        return null
    }

    fun dotCallReceiver(dotCallExpr: PsiElement): PsiElement? =
        dotCallExpr.takeIf { isDotCallExpr(it) }?.firstChild

    fun nameAfterDot(dotCallExpr: PsiElement): PsiElement? =
        firstDirectChildOfRule(dotCallExpr, QuintParser.RULE_nameAfterDot)

    fun simpleQualIdInExpression(expr: PsiElement): PsiElement? {
        if (isRule(expr, QuintParser.RULE_qualId)) return expr
        var child = expr.firstChild
        while (child != null) {
            if (isRule(child, QuintParser.RULE_qualId)) return child
            if (isRule(child, QuintParser.RULE_expr)) {
                val nested = simpleQualIdInExpression(child)
                if (nested != null) return nested
            }
            child = child.nextSibling
        }
        return null
    }

    fun firstArgumentExpression(argList: PsiElement): PsiElement? =
        firstDirectChildOfRule(argList, QuintParser.RULE_expr)

    fun isNameNode(element: PsiElement?): Boolean =
        isRule(element, QuintParser.RULE_name)

    fun isImportModule(element: PsiElement?): Boolean =
        isRule(element, QuintParser.RULE_importMod)

    fun isInstanceModule(element: PsiElement?): Boolean =
        isRule(element, QuintParser.RULE_instanceMod)

    fun isOperationDefinition(element: PsiElement?): Boolean =
        isRule(element, QuintParser.RULE_operDef)

    fun nameNodes(importMod: PsiElement): List<PsiElement> =
        directChildrenOfRule(importMod, QuintParser.RULE_name)

    fun documentedDeclarations(module: PsiElement): List<PsiElement> =
        directChildrenOfRule(module, QuintParser.RULE_documentedDeclaration)

    fun declarationNodes(documentedDeclaration: PsiElement): List<PsiElement> =
        directChildrenOfRule(documentedDeclaration, QuintParser.RULE_declaration)

    fun parameterNodes(parent: PsiElement): List<PsiElement> =
        directChildrenOfRule(parent, QuintParser.RULE_parameter)

    fun annotatedParameterNodes(parent: PsiElement): List<PsiElement> =
        directChildrenOfRule(parent, QuintParser.RULE_annotatedParameter)

    fun isInsideNameAfterDot(position: PsiElement, maxDepth: Int = 6): Boolean =
        isInsideRule(position, QuintParser.RULE_nameAfterDot, maxDepth)

    fun isStringToken(element: PsiElement?): Boolean {
        val elType = element?.node?.elementType ?: return false
        return elType is TokenIElementType && elType.toString().contains("STRING")
    }

    fun stringTokenAt(element: PsiElement): PsiElement? {
        if (isStringToken(element)) return element
        val parent = element.parent ?: return null
        return parent.takeIf { isStringToken(it) }
    }

    fun enclosingRule(position: PsiElement, ruleIndex: Int, maxDepth: Int): PsiElement? {
        var current = position.parent
        var depth = 0
        while (current != null && depth < maxDepth) {
            if (isRule(current, ruleIndex)) return current
            current = current.parent
            depth++
        }
        return null
    }

    fun enclosingArgumentList(position: PsiElement, maxDepth: Int = 8): PsiElement? =
        enclosingRule(position, QuintParser.RULE_argList, maxDepth)

    fun isAncestor(ancestor: PsiElement, element: PsiElement): Boolean =
        PsiTreeUtil.isAncestor(ancestor, element, false)

    fun isInsideRule(position: PsiElement, ruleIndex: Int, maxDepth: Int): Boolean {
        var current = position.parent
        var depth = 0
        while (current != null && depth < maxDepth) {
            if (isRule(current, ruleIndex)) return true
            current = current.parent
            depth++
        }
        return false
    }
}
