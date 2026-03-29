package com.dearlordylord.quint.idea.references

import com.dearlordylord.quint.idea.QuintLanguage
import com.intellij.codeInsight.navigation.actions.GotoDeclarationHandler
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiElement
import org.antlr.intellij.adaptor.lexer.TokenIElementType

/**
 * Handles Cmd+Click on string literals inside `.with("fieldName", value)`.
 * Navigates to the field definition in the record type declaration.
 * Uses GotoDeclarationHandler because PsiReferenceContributor doesn't work
 * for ANTLR leaf token elements.
 */
class QuintRecordFieldGotoHandler : GotoDeclarationHandler {

    override fun getGotoDeclarationTargets(
        sourceElement: PsiElement?,
        offset: Int,
        editor: Editor?
    ): Array<PsiElement>? {
        if (sourceElement == null) return null
        if (sourceElement.language != QuintLanguage.INSTANCE) return null

        val stringElement = findStringElement(sourceElement) ?: return null
        val text = stringElement.text
        if (text.length < 2 || !text.startsWith("\"") || !text.endsWith("\"")) return null
        val fieldName = text.substring(1, text.length - 1)

        // Check if the string is the first arg of .with()
        val dotCall = QuintRecordFieldReferenceContributor.findWithDotCall(stringElement) ?: return null

        val fields = QuintRecordTypeResolver.resolveRecordFieldsFromDotCall(dotCall) ?: return null
        if (fields.none { it.fieldName == fieldName }) return null

        val target = QuintRecordTypeResolver.findFieldDefinition(fieldName, fields, stringElement)
            ?: return null

        return arrayOf(target)
    }

    private fun findStringElement(element: PsiElement): PsiElement? {
        // The source element might be the string content or the string token itself
        val elType = element.node?.elementType
        if (elType is TokenIElementType && elType.toString().contains("STRING")) return element

        val parent = element.parent ?: return null
        val parentType = parent.node?.elementType
        if (parentType is TokenIElementType && parentType.toString().contains("STRING")) return parent

        return null
    }
}
