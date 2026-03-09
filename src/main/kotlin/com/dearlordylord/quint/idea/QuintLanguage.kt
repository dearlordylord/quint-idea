package com.dearlordylord.quint.idea

import com.intellij.lang.Language

class QuintLanguage private constructor() : Language("Quint") {
    companion object {
        @JvmField
        val INSTANCE = QuintLanguage()
    }
}
