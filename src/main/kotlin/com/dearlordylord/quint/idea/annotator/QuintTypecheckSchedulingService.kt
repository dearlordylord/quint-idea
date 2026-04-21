package com.dearlordylord.quint.idea.annotator

import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.util.Key
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiManager
import com.intellij.util.Alarm
import java.util.concurrent.ConcurrentHashMap

class QuintTypecheckSchedulingService : Disposable {
    companion object {
        private val LOG = Logger.getInstance(QuintTypecheckSchedulingService::class.java)
        const val QUIET_PERIOD_MS = 750L
        private val LAST_EDIT_AT_KEY = Key.create<Long>("QUINT_LAST_EDIT_AT")

        @Volatile
        var nowProvider: () -> Long = System::currentTimeMillis

        // In tests we don't want to schedule actual alarms (no project + no daemon).
        @Volatile
        var alarmFactory: ((Disposable) -> ScheduledRestarter)? = null

        fun getInstance(): QuintTypecheckSchedulingService =
            ApplicationManager.getApplication().getService(QuintTypecheckSchedulingService::class.java)
    }

    /** A debounced "restart daemon for this file later" command. Lets tests stub it. */
    interface ScheduledRestarter {
        fun scheduleRestart(file: VirtualFile, afterMs: Long)
    }

    private val perFileRestarter: ScheduledRestarter = (alarmFactory?.invoke(this)) ?: AlarmRestarter(this)

    init {
        EditorFactory.getInstance().eventMulticaster.addDocumentListener(
            object : DocumentListener {
                override fun documentChanged(event: DocumentEvent) {
                    val file = FileDocumentManager.getInstance().getFile(event.document) ?: return
                    if (file.extension != "qnt") return
                    LOG.info("documentChanged: path=${file.path} oldLen=${event.oldLength} newLen=${event.newLength} offset=${event.offset}")
                    markEdited(event.document)
                    // Schedule a daemon restart after the quiet period. This is what
                    // unblocks the typecheck after the user stops typing — without it,
                    // the deferred pass completes empty and IntelliJ has no signal to
                    // schedule another one.
                    perFileRestarter.scheduleRestart(file, QUIET_PERIOD_MS + 50)
                }
            },
            this
        )
    }

    fun shouldDefer(document: Document): Boolean {
        val lastEditAt = document.getUserData(LAST_EDIT_AT_KEY) ?: return false
        val now = nowProvider()
        val delta = now - lastEditAt
        val defer = delta < QUIET_PERIOD_MS
        if (defer) {
            val path = FileDocumentManager.getInstance().getFile(document)?.path
            LOG.info("shouldDefer=true path=$path delta=${delta}ms")
        }
        return defer
    }

    internal fun markEdited(document: Document) {
        document.putUserData(LAST_EDIT_AT_KEY, nowProvider())
    }

    override fun dispose() = Unit

    /** Default restarter — debounces per-file via a single Alarm. */
    private class AlarmRestarter(parent: Disposable) : ScheduledRestarter {
        private val alarm = Alarm(Alarm.ThreadToUse.SWING_THREAD, parent)
        private val pending = ConcurrentHashMap<String, Runnable>()

        override fun scheduleRestart(file: VirtualFile, afterMs: Long) {
            val key = file.path
            // Cancel any prior pending restart for this file (debounce).
            pending[key]?.let { alarm.cancelRequest(it) }
            val runnable = Runnable {
                pending.remove(key)
                val project = ProjectManager.getInstance().openProjects
                    .firstOrNull { p -> !p.isDisposed && PsiManager.getInstance(p).findFile(file) != null }
                    ?: return@Runnable
                val psi = PsiManager.getInstance(project).findFile(file) ?: return@Runnable
                LOG.info("scheduled restart firing for ${file.path}")
                DaemonCodeAnalyzer.getInstance(project).restart(psi)
            }
            pending[key] = runnable
            alarm.addRequest(runnable, afterMs.toInt())
        }
    }
}
