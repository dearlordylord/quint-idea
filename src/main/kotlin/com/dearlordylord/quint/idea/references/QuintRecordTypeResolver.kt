package com.dearlordylord.quint.idea.references

import com.dearlordylord.quint.idea.annotator.QuintFieldNode
import com.dearlordylord.quint.idea.annotator.QuintTypeCache
import com.dearlordylord.quint.idea.annotator.QuintTypeFormatter
import com.dearlordylord.quint.idea.annotator.QuintTypeNode
import com.dearlordylord.quint.idea.parser.QuintParser
import com.dearlordylord.quint.idea.psi.QuintPsiUtils
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import org.antlr.intellij.adaptor.lexer.RuleIElementType

/**
 * Resolves record field information from dotCall receiver expressions.
 */
object QuintRecordTypeResolver {

    /**
     * Given a position inside a dotCall (e.g. the nameAfterDot or an argList element),
     * find the enclosing dotCall's receiver and return its record fields.
     */
    fun resolveReceiverRecordFields(position: PsiElement): List<QuintFieldNode>? {
        val dotCall = findEnclosingDotCall(position) ?: return null
        return resolveRecordFieldsFromDotCall(dotCall)
    }

    /**
     * Given a dotCall expr node, resolve the receiver's type and extract record fields.
     */
    fun resolveRecordFieldsFromDotCall(dotCallExpr: PsiElement): List<QuintFieldNode>? {
        val receiverExpr = dotCallExpr.firstChild ?: return null
        val receiverType = resolveExprType(receiverExpr) ?: return null
        return QuintTypeFormatter.collectRecordFields(receiverType)
    }

    /**
     * Find the enclosing dotCall expr node from a position.
     */
    fun findEnclosingDotCall(position: PsiElement): PsiElement? {
        var current = position.parent
        var depth = 0
        while (current != null && depth < 10) {
            val type = current.node?.elementType as? RuleIElementType
            if (type != null && type.ruleIndex == QuintParser.RULE_expr) {
                // Check if this expr is a dotCall: children include '.' and nameAfterDot
                var hasDot = false
                var hasNameAfterDot = false
                var child = current.firstChild
                while (child != null) {
                    if (child.text == ".") hasDot = true
                    val ct = child.node?.elementType as? RuleIElementType
                    if (ct?.ruleIndex == QuintParser.RULE_nameAfterDot) hasNameAfterDot = true
                    if (hasDot && hasNameAfterDot) return current
                    child = child.nextSibling
                }
            }
            current = current.parent
            depth++
        }
        return null
    }

    /**
     * Try to resolve the type of an expression.
     * Currently only handles simple identifier expressions (qualId).
     */
    private fun resolveExprType(expr: PsiElement): QuintTypeNode? {
        val qualId = findQualId(expr) ?: return null

        val ref = QuintReference(qualId, TextRange(0, qualId.textLength))
        val declaration = ref.resolve() ?: return null

        // For annotatedParameter (e.g. `t: TurnState`), resolve via the type annotation
        val annotatedType = resolveAnnotatedParameterType(declaration, expr)
        if (annotatedType != null) return annotatedType

        val declName = QuintPsiUtils.getDeclarationName(declaration)
            ?: QuintPsiUtils.getDeclarationName(declaration.parent)
            ?: return null
        val module = QuintPsiUtils.getContainingModule(declaration) ?: return null
        val moduleName = QuintPsiUtils.getDeclarationName(module) ?: return null

        val declFile = declaration.containingFile?.virtualFile
        val exprFile = expr.containingFile?.virtualFile

        val scheme = declFile?.let { QuintTypeCache.getTypeScheme(it, moduleName, declName) }
            ?: exprFile?.takeIf { it != declFile }?.let { QuintTypeCache.getTypeScheme(it, moduleName, declName) }
            ?: return null

        return scheme.type
    }

    /**
     * For an annotatedParameter, extract the type name from the annotation
     * and look it up in the type cache. Only works for named types (e.g. TurnState),
     * not compound types (e.g. Set[int]) — those aren't record types anyway.
     */
    private fun resolveAnnotatedParameterType(declaration: PsiElement, contextExpr: PsiElement): QuintTypeNode? {
        val typeName = QuintPsiUtils.getAnnotatedParameterTypeNode(declaration)?.text ?: return null

        val module = QuintPsiUtils.getContainingModule(declaration) ?: return null
        val moduleName = QuintPsiUtils.getDeclarationName(module) ?: return null

        val declFile = declaration.containingFile?.virtualFile
        val exprFile = contextExpr.containingFile?.virtualFile

        val scheme = declFile?.let { QuintTypeCache.getTypeScheme(it, moduleName, typeName) }
            ?: exprFile?.takeIf { it != declFile }?.let { QuintTypeCache.getTypeScheme(it, moduleName, typeName) }
            ?: return null

        return scheme.type
    }

    private fun findQualId(expr: PsiElement): PsiElement? {
        val type = expr.node?.elementType as? RuleIElementType
        if (type != null && type.ruleIndex == QuintParser.RULE_qualId) return expr
        // Search direct children only (avoid descending into nested expressions)
        var child = expr.firstChild
        while (child != null) {
            val childType = child.node?.elementType as? RuleIElementType
            if (childType != null && childType.ruleIndex == QuintParser.RULE_qualId) return child
            // Recurse into literalOrId-level sub-expressions but not deeper
            if (childType != null && childType.ruleIndex == QuintParser.RULE_expr) {
                val nested = findQualId(child)
                if (nested != null) return nested
            }
            child = child.nextSibling
        }
        return null
    }

    /**
     * Find the typeDef rowLabel PSI node that defines a given record field.
     * Searches typeDef declarations in the module for a matching rowLabel.
     * When receiverFields are provided, disambiguates by matching the full field set.
     */
    fun findFieldDefinition(
        fieldName: String,
        receiverFields: List<QuintFieldNode>?,
        contextElement: PsiElement
    ): PsiElement? {
        val module = QuintPsiUtils.getContainingModule(contextElement) ?: return null
        val typeDefs = QuintPsiUtils.findChildrenOfRule(module, QuintParser.RULE_typeDef)

        var bestMatch: PsiElement? = null
        val receiverFieldNames = receiverFields?.map { it.fieldName }?.toSet()

        for (typeDef in typeDefs) {
            val rowLabels = QuintPsiUtils.findChildrenOfRule(typeDef, QuintParser.RULE_rowLabel)
            val matchingLabel = rowLabels.firstOrNull { it.text == fieldName } ?: continue

            if (receiverFieldNames == null) {
                return matchingLabel
            }

            // Disambiguate: prefer typeDef whose field set matches the receiver's fields
            val allLabels = rowLabels.map { it.text }.toSet()
            if (allLabels == receiverFieldNames) {
                return matchingLabel
            }
            if (bestMatch == null) {
                bestMatch = matchingLabel
            }
        }

        return bestMatch
    }
}
