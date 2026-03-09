package com.dearlordylord.quint.idea.annotator

import com.dearlordylord.quint.idea.settings.QuintSettingsState
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.process.CapturingProcessHandler
import com.intellij.openapi.diagnostic.Logger
import java.io.File
import java.nio.charset.StandardCharsets

class QuintCliToolRunner : QuintToolRunner {
    companion object {
        private val LOG = Logger.getInstance(QuintCliToolRunner::class.java)
        private const val TIMEOUT_MS = 30_000
    }

    override fun typecheck(filePath: String): QuintTypecheckResult {
        val quintPath = QuintSettingsState.getInstance().quintBinaryPath
        if (quintPath.isBlank()) {
            return QuintTypecheckResult(stage = "typechecking", errors = emptyList(), warnings = emptyList())
        }

        val quintFile = File(quintPath)
        if (!quintFile.exists() || !quintFile.canExecute()) {
            LOG.warn("Quint binary not found or not executable: $quintPath")
            return QuintTypecheckResult(stage = "typechecking", errors = emptyList(), warnings = emptyList())
        }

        val tempFile = File.createTempFile("quint-typecheck-", ".json")
        try {
            val commandLine = GeneralCommandLine(
                quintPath, "typecheck", "--out", tempFile.absolutePath, filePath
            ).withCharset(StandardCharsets.UTF_8)

            val handler = CapturingProcessHandler(commandLine)
            val result = handler.runProcess(TIMEOUT_MS)

            if (result.isTimeout) {
                LOG.warn("Quint typecheck timed out for $filePath")
                return QuintTypecheckResult(stage = "typechecking", errors = emptyList(), warnings = emptyList())
            }

            if (!tempFile.exists() || tempFile.length() == 0L) {
                LOG.warn("Quint typecheck produced no output file for $filePath. stderr: ${result.stderr}")
                return QuintTypecheckResult(stage = "typechecking", errors = emptyList(), warnings = emptyList())
            }

            val json = tempFile.readText(StandardCharsets.UTF_8)
            return try {
                QuintTypecheckResultParser.parse(json)
            } catch (e: Exception) {
                LOG.warn("Failed to parse quint typecheck output: ${e.message}")
                QuintTypecheckResult(stage = "typechecking", errors = emptyList(), warnings = emptyList())
            }
        } finally {
            tempFile.delete()
        }
    }
}
