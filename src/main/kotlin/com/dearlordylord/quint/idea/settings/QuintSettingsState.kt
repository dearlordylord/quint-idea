package com.dearlordylord.quint.idea.settings

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.util.xmlb.XmlSerializerUtil

@State(
    name = "QuintSettings",
    storages = [Storage("QuintSettings.xml")]
)
class QuintSettingsState : PersistentStateComponent<QuintSettingsState> {
    var quintBinaryPath: String = ""

    override fun getState(): QuintSettingsState = this
    override fun loadState(state: QuintSettingsState) {
        XmlSerializerUtil.copyBean(state, this)
    }

    companion object {
        fun getInstance(): QuintSettingsState =
            ApplicationManager.getApplication().getService(QuintSettingsState::class.java)
    }
}
