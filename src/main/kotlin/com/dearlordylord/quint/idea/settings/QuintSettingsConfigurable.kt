package com.dearlordylord.quint.idea.settings

import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import com.intellij.util.ui.FormBuilder
import javax.swing.JComponent
import javax.swing.JPanel

class QuintSettingsConfigurable : Configurable {
    private var panel: JPanel? = null
    private var quintPathField: TextFieldWithBrowseButton? = null

    override fun getDisplayName(): String = "Quint"

    override fun createComponent(): JComponent {
        quintPathField = TextFieldWithBrowseButton().apply {
            addBrowseFolderListener(
                "Select Quint Binary",
                "Path to the quint executable",
                null,
                FileChooserDescriptorFactory.createSingleFileDescriptor()
            )
        }

        panel = FormBuilder.createFormBuilder()
            .addLabeledComponent("Quint binary path:", quintPathField!!)
            .addComponentFillVertically(JPanel(), 0)
            .panel

        return panel!!
    }

    override fun isModified(): Boolean {
        val settings = QuintSettingsState.getInstance()
        return quintPathField?.text != settings.quintBinaryPath
    }

    override fun apply() {
        val settings = QuintSettingsState.getInstance()
        settings.quintBinaryPath = quintPathField?.text ?: ""
    }

    override fun reset() {
        val settings = QuintSettingsState.getInstance()
        quintPathField?.text = settings.quintBinaryPath
    }

    override fun disposeUIResources() {
        panel = null
        quintPathField = null
    }
}
