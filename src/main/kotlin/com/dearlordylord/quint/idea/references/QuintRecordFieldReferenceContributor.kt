package com.dearlordylord.quint.idea.references

import com.dearlordylord.quint.idea.QuintLanguage
import com.intellij.patterns.PlatformPatterns
import com.intellij.psi.*
import com.intellij.util.ProcessingContext

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
            val ref = QuintRecordTypeResolver.referenceForWithString(element) ?: return PsiReference.EMPTY_ARRAY
            return arrayOf(ref)
        }
    }

    companion object {
        /**
         * If [stringElement] is the first argument of a `.with()` dotCall, return that dotCall.
         * Shared between the reference contributor and the string completion provider.
         */
        fun findWithDotCall(stringElement: PsiElement): PsiElement? {
            return QuintRecordTypeResolver.withDotCallForFirstStringArgument(stringElement)
        }
    }
}
