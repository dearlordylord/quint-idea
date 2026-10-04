package com.dearlordylord.quint.idea.annotator

import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.ProjectLocator
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiManager
import com.intellij.util.ui.update.MergingUpdateQueue
import com.intellij.util.ui.update.Update
import org.jetbrains.annotations.TestOnly

class QuintTypecheckSchedulingService : Disposable {
    companion object {
        const val QUIET_PERIOD_MS = 750L
        private val LAST_EDIT_AT_KEY = Key.create<Long>("QUINT_LAST_EDIT_AT")

        @TestOnly @Volatile
        var nowProvider: () -> Long = System::currentTimeMillis

        fun getInstance(): QuintTypecheckSchedulingService =
            ApplicationManager.getApplication().getService(QuintTypecheckSchedulingService::class.java)
    }

    // Per-file debounced daemon restarter. Without this, a pass that defers completes
    // empty and IntelliJ has no signal to run another — the typecheck gets stuck.
    private val restartQueue = MergingUpdateQueue(
        "QuintTypecheckRestart", QUIET_PERIOD_MS.toInt() + 50, true, null, this
    )

    init {
        EditorFactory.getInstance().eventMulticaster.addDocumentListener(
            object : DocumentListener {
                override fun documentChanged(event: DocumentEvent) {
                    val file = FileDocumentManager.getInstance().getFile(event.document) ?: return
                    if (file.extension != "qnt") return
                    markEdited(event.document)
                    for (project in ProjectManager.getInstance().openProjects) {
                        QuintCheckingService.getInstance(project).invalidate(file.path)
                    }
                    ProjectLocator.getInstance().guessProjectForFile(file)?.let { scheduleRestart(it, file) }
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

    fun scheduleRestart(project: Project, file: VirtualFile) {
        restartQueue.queue(object : Update(project to file.path) {
            override fun run() {
                if (project.isDisposed || !file.isValid) return
                val psi = PsiManager.getInstance(project).findFile(file) ?: return
                DaemonCodeAnalyzer.getInstance(project).restart(psi)
            }
        })
    }

    override fun dispose() = Unit
}
