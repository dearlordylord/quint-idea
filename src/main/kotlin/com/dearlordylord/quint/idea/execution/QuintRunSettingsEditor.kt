package com.dearlordylord.quint.idea.execution

import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import com.intellij.util.ui.FormBuilder
import javax.swing.*

class QuintRunSettingsEditor : SettingsEditor<QuintRunConfiguration>() {
    private val source = TextFieldWithBrowseButton().apply {
        addBrowseFolderListener(null, FileChooserDescriptorFactory.singleFile().withTitle("Select Quint Root"))
    }
    private val mode = JComboBox(QuintExecutionMode.entries.toTypedArray())
    private val main = JTextField()
    private val init = JTextField()
    private val step = JTextField()
    private val test = JTextField()
    private val samples = JSpinner(SpinnerNumberModel(100, 1, Int.MAX_VALUE, 1))
    private val steps = JSpinner(SpinnerNumberModel(20, 1, Int.MAX_VALUE, 1))

    override fun createEditor(): JComponent = FormBuilder.createFormBuilder()
        .addComponent(JLabel("Executes saved files. Save changes before running."))
        .addLabeledComponent("Root .qnt file:", source)
        .addLabeledComponent("Workflow:", mode)
        .addLabeledComponent("Main module (optional):", main)
        .addLabeledComponent("Initializer (run):", init)
        .addLabeledComponent("Step action (run):", step)
        .addLabeledComponent("Test name (optional):", test)
        .addLabeledComponent("Maximum samples:", samples)
        .addLabeledComponent("Maximum steps (run):", steps).panel

    override fun resetEditorFrom(config: QuintRunConfiguration) {
        source.text = config.sourcePath; mode.selectedItem = config.mode; main.text = config.mainModule
        init.text = config.initAction; step.text = config.stepAction
        test.text = config.testName; samples.value = config.maxSamples; steps.value = config.maxSteps
    }
    override fun applyEditorTo(config: QuintRunConfiguration) {
        config.sourcePath = source.text; config.mode = mode.selectedItem as QuintExecutionMode; config.mainModule = main.text
        config.initAction = init.text; config.stepAction = step.text
        config.testName = test.text; config.maxSamples = samples.value as Int; config.maxSteps = steps.value as Int
    }
}
