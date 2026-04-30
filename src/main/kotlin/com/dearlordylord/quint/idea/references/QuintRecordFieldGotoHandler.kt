package com.dearlordylord.quint.idea.references

import com.dearlordylord.quint.idea.QuintLanguage
import com.intellij.codeInsight.navigation.actions.GotoDeclarationHandler
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiElement

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

        return QuintRecordTypeResolver.gotoTargetsForWithString(sourceElement)
    }
}
