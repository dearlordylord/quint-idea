package com.dearlordylord.quint.idea.settings

import com.intellij.openapi.options.Configurable
import javax.swing.JComponent

class QuintSettingsConfigurable : Configurable {
    override fun getDisplayName(): String = "Quint"
    override fun createComponent(): JComponent? = null
    override fun isModified(): Boolean = false
    override fun apply() {}
}
