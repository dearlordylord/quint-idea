package com.dearlordylord.quint.idea.annotator

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.openapi.util.Key

class QuintTypecheckSchedulingService : Disposable {
    companion object {
        private const val QUIET_PERIOD_MS = 750L
        private val LAST_EDIT_AT_KEY = Key.create<Long>("QUINT_LAST_EDIT_AT")

        @Volatile
        var nowProvider: () -> Long = System::currentTimeMillis

        fun getInstance(): QuintTypecheckSchedulingService =
            ApplicationManager.getApplication().getService(QuintTypecheckSchedulingService::class.java)
    }

    init {
        EditorFactory.getInstance().eventMulticaster.addDocumentListener(
            object : DocumentListener {
                override fun documentChanged(event: DocumentEvent) {
                    markEdited(event.document)
                }
            },
            this
        )
    }

    fun shouldDefer(document: Document): Boolean {
        val lastEditAt = document.getUserData(LAST_EDIT_AT_KEY) ?: return false
        return nowProvider() - lastEditAt < QUIET_PERIOD_MS
    }

    internal fun markEdited(document: Document) {
        document.putUserData(LAST_EDIT_AT_KEY, nowProvider())
    }

    override fun dispose() = Unit
}
