package com.dearlordylord.quint.idea

import com.intellij.openapi.fileTypes.LanguageFileType
import javax.swing.Icon

class QuintFileType private constructor() : LanguageFileType(QuintLanguage.INSTANCE) {
    override fun getName(): String = "Quint"
    override fun getDescription(): String = "Quint specification file"
    override fun getDefaultExtension(): String = "qnt"
    override fun getIcon(): Icon = QuintIcons.FILE

    companion object {
        @JvmField
        val INSTANCE = QuintFileType()
    }
}
