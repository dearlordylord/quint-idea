package com.dearlordylord.quint.idea.settings

import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import com.intellij.util.ui.FormBuilder
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.JCheckBox
import com.intellij.openapi.project.ProjectManager
import com.dearlordylord.quint.idea.annotator.QuintCheckingService

class QuintSettingsConfigurable : Configurable {
    private var panel: JPanel? = null
    private var backgroundCheck: JCheckBox? = null
    private var quintPathField: TextFieldWithBrowseButton? = null

    override fun getDisplayName(): String = "Quint"

    override fun createComponent(): JComponent {
        quintPathField = TextFieldWithBrowseButton().apply {
            addBrowseFolderListener(
                null,
                FileChooserDescriptorFactory.singleFile()
                    .withTitle("Select Quint Binary")
                    .withDescription("Path to the quint executable")
            )
        }

        backgroundCheck = JCheckBox("Check Quint files in the background")
        panel = FormBuilder.createFormBuilder()
            .addLabeledComponent("Quint binary path:", quintPathField!!)
            .addComponent(backgroundCheck!!)
            .addComponentFillVertically(JPanel(), 0)
            .panel

        return panel!!
    }

    override fun isModified(): Boolean {
        val settings = QuintSettingsState.getInstance()
        return quintPathField?.text != settings.quintBinaryPath || backgroundCheck?.isSelected != settings.backgroundChecking
    }

    override fun apply() {
        val settings = QuintSettingsState.getInstance()
        settings.quintBinaryPath = quintPathField?.text ?: ""
        settings.backgroundChecking = backgroundCheck?.isSelected ?: true
        ProjectManager.getInstance().openProjects.forEach { QuintCheckingService.getInstance(it).invalidate() }
    }

    override fun reset() {
        val settings = QuintSettingsState.getInstance()
        quintPathField?.text = settings.quintBinaryPath
        backgroundCheck?.isSelected = settings.backgroundChecking
    }

    override fun disposeUIResources() {
        panel = null
        quintPathField = null
        backgroundCheck = null
    }
}
