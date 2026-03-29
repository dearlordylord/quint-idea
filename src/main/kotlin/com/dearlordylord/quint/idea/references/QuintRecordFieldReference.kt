package com.dearlordylord.quint.idea.references

import com.dearlordylord.quint.idea.annotator.QuintFieldNode
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReferenceBase

/**
 * Reference from a string literal (e.g. "fieldName" in `.with("fieldName", value)`)
 * to the record field definition in the type declaration.
 */
class QuintRecordFieldReference(
    element: PsiElement,
    textRange: TextRange,
    private val fieldName: String,
    private val receiverFields: List<QuintFieldNode>
) : PsiReferenceBase<PsiElement>(element, textRange) {

    override fun resolve(): PsiElement? {
        return QuintRecordTypeResolver.findFieldDefinition(fieldName, receiverFields, element)
    }

    override fun getVariants(): Array<Any> = emptyArray()
}
