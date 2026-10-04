package com.dearlordylord.quint.idea.annotator

import com.dearlordylord.quint.idea.settings.QuintSettingsState
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.process.CapturingProcessHandler
import com.intellij.openapi.progress.EmptyProgressIndicator
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
import java.io.File
import java.nio.charset.StandardCharsets

class QuintCliToolRunner(
    private val executable: String? = QuintSettingsState.getInstance().resolveQuintPath(),
    private val project: Project? = null
) : QuintToolRunner {
    override fun typecheck(filePath: String): QuintTypecheckResult {
        val path = executable ?: throw QuintToolFailure(QuintCheckingStatus.UNAVAILABLE, "Configure a Quint executable in Settings → Tools → Quint")
        if (!File(path).isFile || !File(path).canExecute()) {
            throw QuintToolFailure(QuintCheckingStatus.UNAVAILABLE, "Quint executable is missing or not executable: $path")
        }
        val tempFile = File.createTempFile("quint-typecheck-", ".json")
        var handler: CapturingProcessHandler? = null
        val checking = project?.let { QuintCheckingService.getInstance(it) }
        try {
            handler = CapturingProcessHandler(GeneralCommandLine(path, "typecheck", "--out", tempFile.absolutePath, filePath)
                .withWorkDirectory(File(filePath).parentFile).withCharset(StandardCharsets.UTF_8))
            handler.setShouldDestroyProcessRecursively(true)
            checking?.attach(handler)
            val output = handler.runProcessWithProgressIndicator(ProgressManager.getInstance().progressIndicator ?: EmptyProgressIndicator(), 30_000)
            if (output.isCancelled) throw ProcessCanceledException()
            if (output.isTimeout) throw QuintToolFailure(QuintCheckingStatus.TIMEOUT, "Quint typecheck exceeded 30 seconds")
            if (tempFile.length() == 0L) throw QuintToolFailure(QuintCheckingStatus.FAILED, "Quint produced no analysis output (exit ${output.exitCode}): ${output.stderr.take(500)}")
            val result = try {
                QuintTypecheckResultParser.parse(tempFile.readText(StandardCharsets.UTF_8))
            } catch (e: IllegalArgumentException) {
                throw QuintToolFailure(QuintCheckingStatus.FAILED, "Invalid Quint output: ${e.message}")
            }
            if (result.stage.isBlank() || (output.exitCode == 0 && result.modules.isEmpty()) || (output.exitCode != 0 && result.errors.isEmpty())) {
                throw QuintToolFailure(QuintCheckingStatus.FAILED, "Quint check failed (exit ${output.exitCode}): ${output.stderr.take(500)}")
            }
            return result
        } finally {
            handler?.let {
                if (!it.isProcessTerminated) it.destroyProcess()
                checking?.detach(it)
            }
            tempFile.delete()
        }
    }
}
