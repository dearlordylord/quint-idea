package com.dearlordylord.quint.idea.editor

import com.dearlordylord.quint.idea.QuintFileType
import com.intellij.codeInsight.editorActions.TypedHandlerDelegate
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.EditorModificationUtil
import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile

class QuintQuoteHandler : TypedHandlerDelegate() {

    override fun beforeCharTyped(c: Char, project: Project, editor: Editor, file: PsiFile, fileType: FileType): Result {
        if (fileType !is QuintFileType || c != '"') return Result.CONTINUE
        if (editor.selectionModel.hasSelection()) return Result.CONTINUE

        val offset = editor.caretModel.offset
        val text = editor.document.charsSequence

        // If next char is already `"`, skip over it instead of inserting
        if (offset < text.length && text[offset] == '"') {
            EditorModificationUtil.moveCaretRelatively(editor, 1)
            return Result.STOP
        }

        // Insert closing quote; platform will insert the opening one
        EditorModificationUtil.insertStringAtCaret(editor, "\"", false, 0)
        return Result.CONTINUE
    }
}
