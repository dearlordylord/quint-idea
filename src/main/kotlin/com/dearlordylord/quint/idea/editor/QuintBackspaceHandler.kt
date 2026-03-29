package com.dearlordylord.quint.idea.editor

import com.dearlordylord.quint.idea.QuintFileType
import com.intellij.codeInsight.editorActions.BackspaceHandlerDelegate
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiFile

class QuintBackspaceHandler : BackspaceHandlerDelegate() {

    override fun beforeCharDeleted(c: Char, file: PsiFile, editor: Editor) {
        // nothing needed before deletion
    }

    override fun charDeleted(c: Char, file: PsiFile, editor: Editor): Boolean {
        if (file.fileType !is QuintFileType || c != '"') return false

        val offset = editor.caretModel.offset
        val text = editor.document.charsSequence
        if (offset < text.length && text[offset] == '"') {
            editor.document.deleteString(offset, offset + 1)
            return true
        }
        return false
    }
}
