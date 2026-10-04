package com.dearlordylord.quint.idea.annotator

import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import org.junit.Assume
import java.io.File
import java.nio.file.Files

/** External CLI seam: opt in with QUINT_TEST_EXECUTABLE and run the required realCliTest gate. */
class QuintRealCliTest : BasePlatformTestCase() {
    private lateinit var executable: String
    private lateinit var directory: File
    private var originalExecutable = ""

    override fun setUp() {
        Assume.assumeTrue("Real CLI fixtures require QUINT_TEST_EXECUTABLE", !System.getenv("QUINT_TEST_EXECUTABLE").isNullOrBlank())
        super.setUp()
        executable = System.getenv("QUINT_TEST_EXECUTABLE")
        originalExecutable = com.dearlordylord.quint.idea.settings.QuintSettingsState.getInstance().quintBinaryPath
        com.dearlordylord.quint.idea.settings.QuintSettingsState.getInstance().quintBinaryPath = executable
        directory = Files.createTempDirectory("quint-cli-fixture-").toFile()
    }

    override fun tearDown() {
        try {
            if (::directory.isInitialized) {
                directory.deleteRecursively()
                com.dearlordylord.quint.idea.settings.QuintSettingsState.getInstance().quintBinaryPath = originalExecutable
            }
        } finally { super.tearDown() }
    }

    private fun source(name: String, text: String): File = File(directory, name).apply {
        parentFile.mkdirs(); writeText(text)
    }

    private fun check(root: File): QuintTypecheckResult = QuintTypecheckExecutor(QuintCliToolRunner(executable)).typecheck(
        QuintAnalysisSnapshot.capture(root.path, root.readText(), executable)
    )

    private fun launch(config: com.dearlordylord.quint.idea.execution.QuintRunConfiguration): Pair<com.intellij.execution.process.ProcessHandler, StringBuilder> {
        val executor = com.intellij.execution.executors.DefaultRunExecutor.getRunExecutorInstance()
        val runner = com.intellij.execution.runners.ProgramRunner.getRunner(executor.id, config)!!
        val environment = com.intellij.execution.runners.ExecutionEnvironmentBuilder.create(config.project, executor, config).build()
        val execution = config.getState(executor, environment).execute(executor, runner)!!
        val handler = execution.processHandler!!
        val output = StringBuilder()
        handler.addProcessListener(object : com.intellij.execution.process.ProcessAdapter() {
            override fun onTextAvailable(event: com.intellij.execution.process.ProcessEvent, outputType: com.intellij.openapi.util.Key<*>) {
                synchronized(output) { output.append(event.text) }
            }
        })
        handler.startNotify()
        execution.executionConsole?.let { com.intellij.openapi.util.Disposer.register(testRootDisposable, it) }
        return handler to output
    }

    private fun configuration(file: File): com.dearlordylord.quint.idea.execution.QuintRunConfiguration {
        val type = com.intellij.execution.configurations.ConfigurationTypeUtil.findConfigurationType(com.dearlordylord.quint.idea.execution.QuintRunConfigurationType::class.java)
        return (type.configurationFactories.single().createTemplateConfiguration(project) as com.dearlordylord.quint.idea.execution.QuintRunConfiguration).apply {
            sourcePath = file.path; mainModule = "selected"; maxSamples = 1; maxSteps = 2
        }
    }

    fun testNativeTestConfigurationLaunchesSelectedModuleAndTest() {
        val root = source("directory with spaces/main.qnt", "module selected { pure val truthTest = true pure val failureTest = false }")
        val config = configuration(root).apply { testName = "truthTest" }
        val (handler, output) = launch(config)
        assertTrue(handler.waitFor(30_000))
        assertEquals(0, handler.exitCode)
        assertTrue(output.toString(), output.contains("truthTest"))
        assertFalse(output.contains("failureTest"))
    }

    fun testNativeRunConfigurationUsesInitializerAndStep() {
        val root = source("directory with spaces/main.qnt", "module selected { var x: int action boot = x' = 0 action tick = x' = x + 1 }")
        val config = configuration(root).apply {
            mode = com.dearlordylord.quint.idea.execution.QuintExecutionMode.RUN
            initAction = "boot"; stepAction = "tick"
        }
        val (handler, output) = launch(config)
        assertTrue(handler.waitFor(30_000))
        assertEquals(output.toString(), 0, handler.exitCode)
    }

    fun testStoppingNativeRunTerminatesItsProcess() {
        val root = source("main.qnt", "module selected { var x: int action init = x' = 0 action step = x' = x + 1 }")
        val config = configuration(root).apply {
            mode = com.dearlordylord.quint.idea.execution.QuintExecutionMode.RUN
            maxSamples = Int.MAX_VALUE; maxSteps = Int.MAX_VALUE
        }
        val (handler, _) = launch(config)
        handler.destroyProcess()
        assertTrue("Stop must terminate the owned process", handler.waitFor(10_000))
        assertTrue(handler.isProcessTerminated)
    }

    fun testProjectDisposalTerminatesNativeRun() {
        val root = source("main.qnt", "module selected { var x: int action init = x' = 0 action step = x' = x + 1 }")
        val isolated = com.intellij.openapi.project.ProjectManager.getInstance().createProject("Quint lifecycle fixture", File(directory, "project").path)!!
        try {
            val type = com.intellij.execution.configurations.ConfigurationTypeUtil.findConfigurationType(com.dearlordylord.quint.idea.execution.QuintRunConfigurationType::class.java)
            val config = (type.configurationFactories.single().createTemplateConfiguration(isolated) as com.dearlordylord.quint.idea.execution.QuintRunConfiguration).apply {
                sourcePath = root.path; mainModule = "selected"
                mode = com.dearlordylord.quint.idea.execution.QuintExecutionMode.RUN
                maxSamples = Int.MAX_VALUE; maxSteps = Int.MAX_VALUE
            }
            val (handler, _) = launch(config)
            com.intellij.openapi.application.WriteAction.run<RuntimeException> { com.intellij.openapi.util.Disposer.dispose(isolated) }
            assertTrue("closing project must end execution", handler.waitFor(10_000))
            assertTrue(handler.isProcessTerminated)
        } finally {
            if (!isolated.isDisposed) com.intellij.openapi.application.WriteAction.run<RuntimeException> { com.intellij.openapi.util.Disposer.dispose(isolated) }
        }
    }

    fun testMalformedAndEmptyExternalOutputAreExplicitFailures() {
        val root = source("main.qnt", "module main { pure val x = 1 }")
        for ((name, output) in listOf("malformed" to "not-json", "empty" to "")) {
            val script = source("$name.sh", """#!/bin/sh
                previous=''
                for argument in "${'$'}@"; do
                  if [ "${'$'}previous" = '--out' ]; then printf '%s' '$output' > "${'$'}argument"; fi
                  previous="${'$'}argument"
                done
            """.trimIndent())
            assertTrue(script.setExecutable(true))
            try {
                QuintCliToolRunner(script.path).typecheck(root.path)
                fail("$name output must not be accepted")
            } catch (failure: QuintToolFailure) {
                assertEquals(QuintCheckingStatus.FAILED, failure.status)
            }
        }
    }

    fun testExternalCheckerDeadlineReportsTimeout() {
        val root = source("main.qnt", "module main { pure val x = 1 }")
        val script = source("slow.sh", "#!/bin/sh\nsleep 60\n")
        assertTrue(script.setExecutable(true))
        val started = System.nanoTime()
        try {
            QuintCliToolRunner(script.path).typecheck(root.path)
            fail("slow checker must time out")
        } catch (failure: QuintToolFailure) {
            assertEquals(QuintCheckingStatus.TIMEOUT, failure.status)
            assertTrue("deadline must bound subprocess work", (System.nanoTime() - started) / 1_000_000 < 40_000)
        }
    }

    fun testNestedTransitiveParentImportsUseUnsavedDocuments() {
        val helper = source("shared/helper.qnt", "module helper { pure val value = 1 }")
        source("lib/bridge.qnt", "module bridge { import helper.* from \"../shared/helper\" pure val answer = value }")
        val root = source("main.qnt", "module main { import bridge.* from \"lib/bridge\" pure val result = answer }")
        val vf = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(helper)!!
        val document = FileDocumentManager.getInstance().getDocument(vf)!!
        WriteCommandAction.runWriteCommandAction(project) { document.setText("module helper { pure val value: int = true }") }
        val result = check(root)
        assertTrue("unsaved imported type error must be observed", result.errors.isNotEmpty())
        assertTrue("imported diagnostic must map to original source", result.errors.flatMap { it.locs }.any { it.source == helper.path })
        assertEquals("module helper { pure val value = 1 }", helper.readText())
    }

    fun testRemovedImportCannotSurvivePreviousCheckingWorkspace() {
        val helper = source("helper.qnt", "module helper { pure val value = 1 }")
        val root = source("main.qnt", "module main { import helper.* from \"helper\" pure val answer = value }")
        assertTrue(check(root).errors.isEmpty())
        assertTrue(helper.delete())
        assertTrue("deleted dependency must be reported", check(root).errors.isNotEmpty())
    }

    fun testCyclesReturnLoadingErrorsInsteadOfHanging() {
        val root = source("main.qnt", "module main { import helper.* from \"helper\" pure val x = 1 }")
        source("helper.qnt", "module helper { import main.* from \"main\" pure val y = 2 }")
        assertTrue(check(root).errors.isNotEmpty())
    }
    fun testPinnedCompilerConfirmsDeclarationSurfaces() {
        val validSources = listOf(
            "module Counter { const N: int pure val step = N } module Main { import Counter(N = 1) as C pure val result = C::step }",
            "module Counter { const N: int pure val step = N } module Main { import Counter(N = 1).* pure val result = step }",
            "module A { pure val x = 1 } module B { import A export A } module Main { import B pure val result = B::A::x }",
            "module A { pure val x = 1 } module B { import A export A.* } module Main { import B.* pure val result = x }",
            "module M { type T = Some(int) | None pure def f(v: T): int = match v { Some(n) => n | None => 0 } }"
        )
        for ((index, text) in validSources.withIndex()) {
            val result = check(source("scope$index.qnt", text))
            assertTrue("compiler rejected editor scope fixture: $text: ${result.errors}", result.errors.isEmpty())
        }
        val hidden = source("hidden.qnt", "module A { pure val x = 1 } module B { import A.* } module Main { import B.* pure val result = x }")
        assertTrue("ordinary imports must not be re-exported", check(hidden).errors.isNotEmpty())
    }
}
