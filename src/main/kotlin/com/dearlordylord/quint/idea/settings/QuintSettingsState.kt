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

    @Transient
    @Volatile
    private var detectedPath: String? = null
    @Transient
    @Volatile
    private var detectionAttempted: Boolean = false

    fun resolveQuintPath(): String? {
        if (quintBinaryPath.isNotBlank()) return quintBinaryPath
        if (detectionAttempted) return detectedPath
        synchronized(this) {
            if (detectionAttempted) return detectedPath
            detectedPath = QuintBinaryDetector.detect()
            detectionAttempted = true
        }
        return detectedPath
    }

    override fun getState(): QuintSettingsState = this
    override fun loadState(state: QuintSettingsState) {
        XmlSerializerUtil.copyBean(state, this)
        detectionAttempted = false
        detectedPath = null
    }

    companion object {
        fun getInstance(): QuintSettingsState =
            ApplicationManager.getApplication().getService(QuintSettingsState::class.java)
    }
}
