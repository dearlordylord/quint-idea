package com.dearlordylord.quint.idea.annotator

import com.dearlordylord.quint.idea.QuintFileType
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.ui.Messages

class QuintCheckCurrentFileAction : AnAction() {
    override fun getActionUpdateThread() = ActionUpdateThread.BGT
    override fun update(e: AnActionEvent) {
        e.presentation.isEnabledAndVisible = e.project != null && e.getData(CommonDataKeys.PSI_FILE)?.fileType == QuintFileType.INSTANCE
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val file = e.getData(CommonDataKeys.PSI_FILE) ?: return
        val annotator = QuintExternalAnnotator()
        val input = ReadAction.compute<QuintAnnotatorInput?, RuntimeException> { annotator.collectForManualCheck(file) } ?: return
        ProgressManager.getInstance().run(object : Task.Backgroundable(project, "Checking Quint: ${file.name}", true) {
            override fun run(indicator: ProgressIndicator) {
                try {
                    annotator.doAnnotate(input)
                } finally {
                    file.virtualFile?.let { input.checking.restart(it) }
                }
            }
        })
    }
}

class QuintCheckStatusAction : AnAction() {
    override fun getActionUpdateThread() = ActionUpdateThread.BGT
    override fun update(e: AnActionEvent) {
        e.presentation.isEnabledAndVisible = e.project != null && e.getData(CommonDataKeys.PSI_FILE)?.fileType == QuintFileType.INSTANCE
    }
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val file = e.getData(CommonDataKeys.PSI_FILE)?.virtualFile ?: return
        val state = ReadAction.compute<QuintCheckState?, RuntimeException> { QuintCheckingService.getInstance(project).state(file) }
        val status = state?.status ?: QuintCheckingStatus.UNCHECKED
        val details = state?.message ?: state?.result?.let { "${it.errors.size} errors, ${it.warnings.size} warnings" } ?: "Use Tools → Check Current Quint File"
        Messages.showInfoMessage(project, "${status.label}\n$details", "Quint Check Status")
    }
}
