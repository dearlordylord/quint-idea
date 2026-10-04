package com.dearlordylord.quint.idea.annotator

import com.dearlordylord.quint.idea.settings.QuintSettingsState
import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.openapi.vfs.newvfs.BulkFileListener
import com.intellij.openapi.vfs.newvfs.events.VFileEvent
import com.intellij.psi.PsiManager
import java.util.concurrent.ConcurrentHashMap
import com.intellij.util.Alarm
import java.io.File

enum class QuintCheckingStatus(val label: String) {
    UNCHECKED("Not checked"), CHECKING("Checking"), CURRENT("Check current"), STALE("Check stale"),
    FAILED("Check failed"), UNAVAILABLE("Checker unavailable"), TIMEOUT("Check timed out")
}

class QuintToolFailure(val status: QuintCheckingStatus, message: String) : RuntimeException(message)

data class QuintCheckState(
    val file: VirtualFile,
    val snapshot: QuintAnalysisSnapshot,
    val generation: Long,
    val status: QuintCheckingStatus,
    val result: QuintTypecheckResult? = null,
    val message: String? = null
)

/** Project-owned checking state shared by the annotator, actions and source invalidation. */
class QuintCheckingService(private val project: Project) : Disposable {
    private val states = ConcurrentHashMap<String, QuintCheckState>()
    private var generation = 0L
    private val processes = mutableSetOf<com.intellij.execution.process.ProcessHandler>()
    @Volatile private var disposed = false
    private val externalChanges = Alarm(Alarm.ThreadToUse.POOLED_THREAD, this)

    private fun pollExternalChanges() {
        if (project.isDisposed || disposed) return
        // VFS/document events are immediate; polling also observes unregistered paths,
        // replacement of an external executable, and externally-created missing imports.
        try {
            val changed = ReadAction.compute<List<String>, RuntimeException> {
                val stamp = QuintAnalysisSnapshot.toolStamp(QuintSettingsState.getInstance().resolveQuintPath())
                states.values.filter { entry ->
                    entry.status != QuintCheckingStatus.STALE && entry.file.isValid &&
                        !entry.snapshot.isCurrent(FileDocumentManager.getInstance().getDocument(entry.file)?.text ?: "", stamp)
                }.map { it.file.path }
            }
            changed.forEach(::invalidate)
        } catch (failure: java.io.IOException) {
            com.intellij.openapi.diagnostic.Logger.getInstance(QuintCheckingService::class.java)
                .warn("Could not observe Quint checking inputs", failure)
            invalidate()
        } finally {
            if (!project.isDisposed && !disposed) externalChanges.addRequest({ pollExternalChanges() }, 2000)
        }
    }

    @Synchronized
    fun attach(handler: com.intellij.execution.process.ProcessHandler) {
        if (disposed || project.isDisposed) {
            handler.destroyProcess()
            throw com.intellij.openapi.progress.ProcessCanceledException()
        }
        processes.add(handler)
    }

    @Synchronized
    fun detach(handler: com.intellij.execution.process.ProcessHandler) { processes.remove(handler) }

    init {
        externalChanges.addRequest({ pollExternalChanges() }, 2000)
        project.messageBus.connect(this).subscribe(VirtualFileManager.VFS_CHANGES, object : BulkFileListener {
            override fun after(events: List<VFileEvent>) {
                for (event in events) invalidate(event.path)
            }
        })
    }

    @Synchronized
    fun begin(file: VirtualFile, snapshot: QuintAnalysisSnapshot): Long {
        val request = ++generation
        states[file.path] = QuintCheckState(file, snapshot, request, QuintCheckingStatus.CHECKING)
        return request
    }

    fun cached(file: VirtualFile, snapshot: QuintAnalysisSnapshot): QuintTypecheckResult? =
        states[file.path]?.takeIf { it.snapshot == snapshot && it.status in setOf(QuintCheckingStatus.CURRENT, QuintCheckingStatus.FAILED) }?.result

    fun finish(filePath: String, request: Long, result: QuintTypecheckResult?, failure: QuintToolFailure? = null): Boolean {
        val previous = states[filePath] ?: return false
        val fresh = ReadAction.compute<Boolean, RuntimeException> {
            previous.file.isValid && previous.snapshot.isCurrent(FileDocumentManager.getInstance().getDocument(previous.file)?.text ?: "")
        }
        synchronized(this) {
            if (project.isDisposed || states[filePath]?.generation != request) return false
            val status = when {
                !fresh -> QuintCheckingStatus.STALE
                failure != null -> failure.status
                result == null -> QuintCheckingStatus.FAILED
                result.errors.isNotEmpty() -> QuintCheckingStatus.FAILED
                else -> QuintCheckingStatus.CURRENT
            }
            states[filePath] = previous.copy(status = status, result = if (fresh) result else null, message = failure?.message)
            if (fresh && failure == null && result != null && result.errors.isEmpty() && result.modules.isNotEmpty() && result.types.isNotEmpty()) {
                QuintTypeCache.update(previous.file, result, previous.snapshot)
            }
            return fresh && failure == null && result != null
        }
    }

    @Synchronized
    fun cancel(filePath: String, request: Long) {
        val current = states[filePath] ?: return
        if (current.generation == request) {
            states[filePath] = current.copy(generation = ++generation, status = QuintCheckingStatus.STALE, result = null, message = "Check cancelled")
        }
    }

    fun state(file: VirtualFile): QuintCheckState? {
        val entry = states[file.path] ?: return null
        val currentText = FileDocumentManager.getInstance().getDocument(file)?.text ?: ""
        return if (entry.status != QuintCheckingStatus.CHECKING && !entry.snapshot.isCurrent(currentText))
            entry.copy(status = QuintCheckingStatus.STALE, result = null) else entry
    }

    fun accepts(file: VirtualFile, request: Long, snapshot: QuintAnalysisSnapshot): Boolean =
        !project.isDisposed && states[file.path]?.generation == request &&
            snapshot.isCurrent(FileDocumentManager.getInstance().getDocument(file)?.text ?: "")

    fun invalidate(path: String? = null) {
        val affected = synchronized(this) {
            states.values.filter { state -> path == null ||
                (state.snapshot.identities.keys + state.snapshot.identities.values + listOfNotNull(state.snapshot.executable)).any { it == path || it.startsWith("$path/") } }
                .map { entry ->
                    states[entry.file.path] = entry.copy(generation = ++generation, status = QuintCheckingStatus.STALE, result = null)
                    entry.file
                }
        }
        for (file in affected) {
            if (QuintSettingsState.getInstance().backgroundChecking) {
                QuintTypecheckSchedulingService.getInstance().scheduleRestart(project, file)
            } else restart(file)
        }
    }

    fun restart(file: VirtualFile) {
        ApplicationManager.getApplication().invokeLater {
            if (!project.isDisposed && file.isValid) {
                PsiManager.getInstance(project).findFile(file)?.let { DaemonCodeAnalyzer.getInstance(project).restart(it) }
            }
        }
    }

    fun clear() = states.clear()
    @Synchronized
    override fun dispose() {
        disposed = true
        processes.forEach { if (!it.isProcessTerminated) it.destroyProcess() }
        processes.clear()
        states.clear()
    }

    companion object {
        fun getInstance(project: Project): QuintCheckingService = project.getService(QuintCheckingService::class.java)
    }
}
