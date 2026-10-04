package com.dearlordylord.quint.idea.execution

import com.intellij.execution.configurations.ConfigurationTypeUtil
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import org.jdom.Element

class QuintRunConfigurationTest : BasePlatformTestCase() {
    fun testTestDeclarationGetsGutterEntryPoint() {
        myFixture.configureByText("main.qnt", "module main { pure val truth<caret>Test = true }")
        val leaf = myFixture.file.findElementAt(myFixture.caretOffset)!!
        assertNotNull(QuintRunLineMarkerContributor().getInfo(leaf))
    }

    fun testOrdinaryDeclarationHasNoGutterEntryPoint() {
        myFixture.configureByText("main.qnt", "module main { pure val ordinary<caret>Name = true }")
        val leaf = myFixture.file.findElementAt(myFixture.caretOffset)!!
        assertNull(QuintRunLineMarkerContributor().getInfo(leaf))
    }

    fun testSavedRunConfigurationRetainsItsWorkflow() {
        val type = ConfigurationTypeUtil.findConfigurationType(QuintRunConfigurationType::class.java)
        val config = type.configurationFactories.single().createTemplateConfiguration(project) as QuintRunConfiguration
        config.sourcePath = "/tmp/project with spaces/main.qnt"
        config.mode = QuintExecutionMode.RUN
        config.mainModule = "Main"
        config.initAction = "start"
        config.stepAction = "advance"
        config.backend = "typescript"
        val stored = Element("configuration")
        config.writeExternal(stored)
        val restored = type.configurationFactories.single().createTemplateConfiguration(project) as QuintRunConfiguration
        restored.readExternal(stored)
        assertEquals("/tmp/project with spaces/main.qnt", restored.sourcePath)
        assertEquals(QuintExecutionMode.RUN, restored.mode)
        assertEquals("Main", restored.mainModule)
        assertEquals("start", restored.initAction)
        assertEquals("advance", restored.stepAction)
        assertEquals("typescript", restored.backend)
    }
}
