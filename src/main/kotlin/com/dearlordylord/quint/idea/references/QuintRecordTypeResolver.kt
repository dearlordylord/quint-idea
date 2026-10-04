package com.dearlordylord.quint.idea.references

import com.dearlordylord.quint.idea.annotator.QuintFieldNode
import com.dearlordylord.quint.idea.annotator.QuintTypeFormatter
import com.dearlordylord.quint.idea.annotator.QuintTypeInfo
import com.dearlordylord.quint.idea.annotator.QuintTypeNode
import com.dearlordylord.quint.idea.annotator.QuintSourceTypes
import com.dearlordylord.quint.idea.parser.QuintParser
import com.dearlordylord.quint.idea.psi.QuintPsiShape
import com.dearlordylord.quint.idea.psi.QuintPsiUtils
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReference

/**
 * Resolves record field information from dotCall receiver expressions.
 */
object QuintRecordTypeResolver {

    data class FieldStringContext(
        val stringElement: PsiElement,
        val fieldName: String
    )

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
        val receiverExpr = QuintPsiShape.dotCallReceiver(dotCallExpr) ?: return null
        val receiverType = resolveExprType(receiverExpr) ?: return null
        return QuintTypeFormatter.collectRecordFields(receiverType)
    }

    /**
     * Find the enclosing dotCall expr node from a position.
     */
    fun findEnclosingDotCall(position: PsiElement): PsiElement? {
        return QuintPsiShape.enclosingDotCall(position)
    }

    fun resolveNameAfterDotField(element: PsiElement, fieldName: String): PsiElement? {
        val dotCall = findEnclosingDotCall(element) ?: return null
        val nameAfterDot = QuintPsiShape.nameAfterDot(dotCall) ?: return null
        if (!QuintPsiShape.isAncestor(nameAfterDot, element) && nameAfterDot != element.parent) return null

        val fields = resolveRecordFieldsFromDotCall(dotCall) ?: return null
        if (fields.none { it.fieldName == fieldName }) return null
        return findFieldDefinition(fieldName, element)
    }

    fun fieldsForWithString(position: PsiElement): List<QuintFieldNode>? {
        val stringElement = QuintPsiShape.stringTokenAt(position) ?: return null
        val dotCall = withDotCallForFirstStringArgument(stringElement) ?: return null
        return resolveRecordFieldsFromDotCall(dotCall)
    }

    fun referenceForWithString(position: PsiElement): PsiReference? {
        val context = withFieldStringContext(position) ?: return null
        return QuintRecordFieldReference(
            context.stringElement,
            TextRange(1, context.stringElement.text.length - 1),
            context.fieldName
        )
    }

    fun gotoTargetsForWithString(position: PsiElement): Array<PsiElement>? {
        val context = withFieldStringContext(position) ?: return null
        val target = findFieldDefinition(context.fieldName, context.stringElement)
            ?: return null
        return arrayOf(target)
    }

    fun withDotCallForFirstStringArgument(stringElement: PsiElement): PsiElement? {
        val argList = QuintPsiShape.enclosingArgumentList(stringElement)
            ?: return null
        val firstExprInArgList = QuintPsiShape.firstArgumentExpression(argList) ?: return null
        if (!QuintPsiShape.isAncestor(firstExprInArgList, stringElement)) return null

        val dotCall = argList.parent ?: return null
        val nameAfterDot = QuintPsiShape.nameAfterDot(dotCall)
        return dotCall.takeIf { nameAfterDot?.text == "with" }
    }

    private fun withFieldStringContext(position: PsiElement): FieldStringContext? {
        val stringElement = QuintPsiShape.stringTokenAt(position) ?: return null
        val fieldName = stringLiteralValue(stringElement) ?: return null
        val dotCall = withDotCallForFirstStringArgument(stringElement) ?: return null
        val fields = resolveRecordFieldsFromDotCall(dotCall) ?: return null
        if (fields.none { it.fieldName == fieldName }) return null
        return FieldStringContext(stringElement, fieldName)
    }

    private fun stringLiteralValue(element: PsiElement): String? {
        val text = element.text
        if (text.length < 2 || !text.startsWith("\"") || !text.endsWith("\"")) return null
        return text.substring(1, text.length - 1)
    }

    /**
     * Try to resolve the type of an expression.
     * Currently only handles simple identifier expressions (qualId).
     */
    private fun resolveExprType(expr: PsiElement): QuintTypeNode? {
        val qualId = QuintPsiShape.simpleQualIdInExpression(expr) ?: return null

        val ref = QuintReference(qualId, TextRange(0, qualId.textLength))
        val declaration = ref.resolve() ?: return null

        return QuintTypeInfo.typeForDeclaration(declaration, expr)
    }

    /** Navigate only through the receiver annotation and its actual typedef references. */
    fun findFieldDefinition(
        fieldName: String,
        contextElement: PsiElement
    ): PsiElement? {
        val dotCall = findEnclosingDotCall(contextElement) ?: return null
        val receiver = QuintPsiShape.dotCallReceiver(dotCall) ?: return null
        val name = QuintPsiShape.simpleQualIdInExpression(receiver) ?: return null
        val declaration = name.reference?.resolve() ?: return null
        val annotation = QuintSourceTypes.annotation(declaration) ?: return null
        val row = QuintSourceTypes.recordRow(annotation) ?: return null
        return QuintPsiShape.directChildrenOfRule(row, QuintParser.RULE_rowLabel).firstOrNull { it.text == fieldName }
    }
}
