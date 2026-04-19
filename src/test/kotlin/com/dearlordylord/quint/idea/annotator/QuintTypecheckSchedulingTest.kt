package com.dearlordylord.quint.idea.annotator

import com.dearlordylord.quint.idea.settings.QuintSettingsState
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.testFramework.fixtures.BasePlatformTestCase

class QuintTypecheckSchedulingTest : BasePlatformTestCase() {
    private lateinit var originalBinaryPath: String

    override fun setUp() {
        super.setUp()
        originalBinaryPath = QuintSettingsState.getInstance().quintBinaryPath
        QuintSettingsState.getInstance().quintBinaryPath = "/bin/true"
        QuintExternalAnnotator.toolRunnerFactory = null
        QuintExternalAnnotator.clearCacheForTests()
    }

    override fun tearDown() {
        try {
            QuintSettingsState.getInstance().quintBinaryPath = originalBinaryPath
            QuintExternalAnnotator.toolRunnerFactory = null
            QuintExternalAnnotator.clearCacheForTests()
            QuintTypecheckSchedulingService.nowProvider = System::currentTimeMillis
        } finally {
            super.tearDown()
        }
    }

    fun testCollectInformationDefersImmediatelyAfterEdit() {
        val file = myFixture.configureByText("test.qnt", "module test {\n  val x = 1\n}")
        val scheduler = QuintTypecheckSchedulingService.getInstance()
        QuintTypecheckSchedulingService.nowProvider = { 1_000L }

        WriteCommandAction.runWriteCommandAction(project) {
            myFixture.editor.document.insertString(myFixture.editor.document.textLength, "\n// comment")
        }

        assertTrue(scheduler.shouldDefer(myFixture.editor.document))
        assertNull(QuintExternalAnnotator().collectInformation(file))
    }

    fun testCollectInformationUsesCacheForSameModificationStamp() {
        val file = myFixture.configureByText("test.qnt", "module test {\n  val x = 1\n}")
        val annotator = QuintExternalAnnotator()
        val expected = QuintTypecheckResult(stage = "typechecking", errors = emptyList(), warnings = emptyList())
        var invocations = 0

        QuintTypecheckSchedulingService.nowProvider = { 10_000L }
        QuintExternalAnnotator.toolRunnerFactory = {
            object : QuintToolRunner {
                override fun typecheck(filePath: String): QuintTypecheckResult {
                    invocations += 1
                    return expected
                }
            }
        }

        val firstInput = annotator.collectInformation(file)
        assertNotNull(firstInput)
        val firstResult = annotator.doAnnotate(firstInput)
        assertEquals(expected, firstResult?.typecheckResult)
        assertEquals(1, invocations)

        val secondInput = annotator.collectInformation(file)
        assertNotNull(secondInput)
        val secondResult = annotator.doAnnotate(secondInput)
        assertEquals(expected, secondResult?.typecheckResult)
        assertEquals(1, invocations)
    }
}
