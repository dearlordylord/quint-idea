package com.dearlordylord.quint.idea.execution

import com.dearlordylord.quint.idea.QuintIcons
import com.dearlordylord.quint.idea.annotator.QuintCheckingService
import com.dearlordylord.quint.idea.settings.QuintSettingsState
import com.intellij.execution.Executor
import com.intellij.execution.configurations.*
import com.intellij.execution.process.KillableColoredProcessHandler
import com.intellij.execution.process.ProcessAdapter
import com.intellij.execution.process.ProcessEvent
import com.intellij.execution.process.ProcessHandler
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.JDOMExternalizerUtil
import com.intellij.openapi.util.NotNullLazyValue
import org.jdom.Element
import java.io.File
import java.nio.charset.StandardCharsets

class QuintRunConfigurationType : SimpleConfigurationType(
    "QuintRunConfiguration", "Quint", "Run Quint tests or simulations (saved files)", NotNullLazyValue.createValue { QuintIcons.FILE }
) {
    override fun createTemplateConfiguration(project: Project) = QuintRunConfiguration(project, this, "Quint")
}

enum class QuintExecutionMode { TEST, RUN }

class QuintRunConfiguration(project: Project, factory: ConfigurationFactory, name: String) : RunConfigurationBase<RunConfigurationOptions>(project, factory, name) {
    var sourcePath = ""
    var mode = QuintExecutionMode.TEST
    var mainModule = ""
    var initAction = "init"
    var stepAction = "step"
    var testName = ""
    var maxSamples = 100
    var maxSteps = 20

    override fun getConfigurationEditor(): SettingsEditor<out RunConfiguration> = QuintRunSettingsEditor()

    override fun checkConfiguration() {
        val source = File(sourcePath)
        if (!source.isAbsolute || !source.isFile || source.extension != "qnt") throw RuntimeConfigurationError("Select an existing absolute .qnt file path")
        val executable = QuintSettingsState.getInstance().resolveQuintPath()
        if (executable == null || !File(executable).isFile || !File(executable).canExecute()) throw RuntimeConfigurationError("Configure an executable Quint binary in Settings → Tools → Quint")
        if (maxSamples <= 0 || maxSteps <= 0) throw RuntimeConfigurationError("Sample and step limits must be positive")
        if (mode == QuintExecutionMode.RUN && (initAction.isBlank() || stepAction.isBlank())) throw RuntimeConfigurationError("Run requires initializer and step action names")
    }

    override fun getState(executor: Executor, environment: ExecutionEnvironment): RunProfileState = object : CommandLineState(environment) {
        override fun startProcess(): ProcessHandler {
            checkConfiguration()
            val command = GeneralCommandLine(QuintSettingsState.getInstance().resolveQuintPath()!!,
                if (mode == QuintExecutionMode.TEST) "test" else "run", sourcePath,
                "--backend", "typescript", "--max-samples", maxSamples.toString())
                .withWorkDirectory(File(sourcePath).parentFile).withCharset(StandardCharsets.UTF_8)
            if (mainModule.isNotBlank()) command.addParameters("--main", mainModule)
            if (mode == QuintExecutionMode.RUN) command.addParameters("--init", initAction, "--step", stepAction, "--max-steps", maxSteps.toString())
            else if (testName.isNotBlank()) command.addParameters("--match", "^${testName.map { if (it in "\\^$.|?*+()[]{}") "\\$it" else it.toString() }.joinToString("")}$")
            val handler = KillableColoredProcessHandler(command)
            handler.setShouldDestroyProcessRecursively(true)
            val checking = QuintCheckingService.getInstance(project)
            checking.attach(handler)
            handler.addProcessListener(object : ProcessAdapter() {
                override fun processTerminated(event: ProcessEvent) { checking.detach(handler) }
            })
            return handler
        }
    }

    override fun readExternal(element: Element) {
        super.readExternal(element)
        sourcePath = JDOMExternalizerUtil.readField(element, "sourcePath", "")
        mode = runCatching { QuintExecutionMode.valueOf(JDOMExternalizerUtil.readField(element, "mode", "TEST")) }.getOrDefault(QuintExecutionMode.TEST)
        mainModule = JDOMExternalizerUtil.readField(element, "mainModule", "")
        initAction = JDOMExternalizerUtil.readField(element, "initAction", "init")
        stepAction = JDOMExternalizerUtil.readField(element, "stepAction", "step")
        testName = JDOMExternalizerUtil.readField(element, "testName", "")
        maxSamples = JDOMExternalizerUtil.readField(element, "maxSamples", "100").toIntOrNull() ?: 100
        maxSteps = JDOMExternalizerUtil.readField(element, "maxSteps", "20").toIntOrNull() ?: 20
    }

    override fun writeExternal(element: Element) {
        super.writeExternal(element)
        mapOf("sourcePath" to sourcePath, "mode" to mode.name, "mainModule" to mainModule,
            "initAction" to initAction, "stepAction" to stepAction,
            "testName" to testName, "maxSamples" to maxSamples.toString(), "maxSteps" to maxSteps.toString())
            .forEach { (key, value) -> JDOMExternalizerUtil.writeField(element, key, value) }
    }
}
