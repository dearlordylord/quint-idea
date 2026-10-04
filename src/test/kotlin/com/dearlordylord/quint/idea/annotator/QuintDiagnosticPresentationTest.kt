package com.dearlordylord.quint.idea.annotator

import com.dearlordylord.quint.idea.settings.QuintSettingsState
import com.intellij.testFramework.fixtures.BasePlatformTestCase

class QuintDiagnosticPresentationTest : BasePlatformTestCase() {
    fun testUnicodeColumnsHighlightTheActualExpression() {
        val settings = QuintSettingsState.getInstance()
        val binary = settings.quintBinaryPath
        settings.quintBinaryPath = "/bin/true"
        QuintTypecheckSchedulingService.nowProvider = { 0 }
        QuintExternalAnnotator.toolRunnerFactory = {
            object : QuintToolRunner {
                override fun typecheck(filePath: String) = QuintTypecheckResult("typechecking", listOf(
                    QuintError("unicode diagnostic", listOf(QuintErrorLocation(filePath, QuintPosition(0, 35, 35), QuintPosition(0, 38, 38))))
                ), emptyList())
            }
        }
        try {
            myFixture.configureByText("main.qnt", "module main { /* 😀 */ val x: int = true }")
            QuintTypecheckSchedulingService.nowProvider = { 10_000 }
            val error = myFixture.doHighlighting().single { it.description == "unicode diagnostic" }
            assertEquals(36, error.startOffset)
            assertEquals(40, error.endOffset)
        } finally {
            settings.quintBinaryPath = binary
            QuintExternalAnnotator.toolRunnerFactory = null
            QuintTypecheckSchedulingService.nowProvider = System::currentTimeMillis
            QuintExternalAnnotator.clearCacheForTests()
        }
    }
}
