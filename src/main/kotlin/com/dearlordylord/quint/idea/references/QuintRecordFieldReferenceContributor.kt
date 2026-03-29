package com.dearlordylord.quint.idea.references

import com.dearlordylord.quint.idea.QuintLanguage
import com.dearlordylord.quint.idea.parser.QuintParser
import com.dearlordylord.quint.idea.psi.QuintPsiUtils
import com.intellij.openapi.util.TextRange
import com.intellij.patterns.PlatformPatterns
import com.intellij.psi.*
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.ProcessingContext
import org.antlr.intellij.adaptor.lexer.RuleIElementType

/**
 * Injects references onto string literals that are field name arguments in `.with("fieldName", value)`.
 * Enables Cmd+Click navigation from the string to the field definition in the record type.
 */
class QuintRecordFieldReferenceContributor : PsiReferenceContributor() {

    override fun registerReferenceProviders(registrar: PsiReferenceRegistrar) {
        registrar.registerReferenceProvider(
            PlatformPatterns.psiElement().withLanguage(QuintLanguage.INSTANCE),
            QuintRecordFieldReferenceProvider()
        )
    }

    private class QuintRecordFieldReferenceProvider : PsiReferenceProvider() {
        override fun getReferencesByElement(element: PsiElement, context: ProcessingContext): Array<PsiReference> {
            // Only handle STRING tokens (toString() because ANTLR and JFlex have separate IElementType instances)
            if (element.node?.elementType?.toString() != "STRING") return PsiReference.EMPTY_ARRAY

            val dotCall = findWithDotCall(element) ?: return PsiReference.EMPTY_ARRAY

            val fields = QuintRecordTypeResolver.resolveRecordFieldsFromDotCall(dotCall)
                ?: return PsiReference.EMPTY_ARRAY

            val text = element.text
            if (text.length < 2 || !text.startsWith("\"") || !text.endsWith("\"")) {
                return PsiReference.EMPTY_ARRAY
            }
            val fieldName = text.substring(1, text.length - 1)
            if (fields.none { it.fieldName == fieldName }) return PsiReference.EMPTY_ARRAY

            val ref = QuintRecordFieldReference(element, TextRange(1, text.length - 1), fieldName, fields)
            return arrayOf(ref)
        }
    }

    companion object {
        /**
         * If [stringElement] is the first argument of a `.with()` dotCall, return that dotCall.
         * Shared between the reference contributor and the string completion provider.
         */
        fun findWithDotCall(stringElement: PsiElement): PsiElement? {
            var current = stringElement.parent
            var depth = 0
            while (current != null && depth < 8) {
                val type = current.node?.elementType as? RuleIElementType
                if (type != null && type.ruleIndex == QuintParser.RULE_argList) {
                    val firstExprInArgList = QuintPsiUtils.findFirstChildOfRule(current, QuintParser.RULE_expr)
                    val isFirstArg = firstExprInArgList != null
                            && PsiTreeUtil.isAncestor(firstExprInArgList, stringElement, false)
                    if (!isFirstArg) return null

                    val dotCall = current.parent ?: return null
                    val nameAfterDot = QuintPsiUtils.findFirstChildOfRule(dotCall, QuintParser.RULE_nameAfterDot)
                    if (nameAfterDot?.text == "with") return dotCall
                    return null
                }
                current = current.parent
                depth++
            }
            return null
        }
    }
}
