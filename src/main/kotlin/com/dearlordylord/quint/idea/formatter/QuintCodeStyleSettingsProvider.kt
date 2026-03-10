package com.dearlordylord.quint.idea.formatter

import com.dearlordylord.quint.idea.QuintLanguage
import com.intellij.lang.Language
import com.intellij.psi.codeStyle.CodeStyleSettingsCustomizable
import com.intellij.psi.codeStyle.CommonCodeStyleSettings
import com.intellij.psi.codeStyle.LanguageCodeStyleSettingsProvider

class QuintCodeStyleSettingsProvider : LanguageCodeStyleSettingsProvider() {
    override fun getLanguage(): Language = QuintLanguage.INSTANCE

    override fun customizeDefaults(
        commonSettings: CommonCodeStyleSettings,
        indentOptions: CommonCodeStyleSettings.IndentOptions
    ) {
        indentOptions.INDENT_SIZE = 2
        indentOptions.TAB_SIZE = 2
        indentOptions.USE_TAB_CHARACTER = false
        indentOptions.CONTINUATION_INDENT_SIZE = 4
    }

    override fun customizeSettings(consumer: CodeStyleSettingsCustomizable, settingsType: SettingsType) {
        // Default UI is sufficient — no custom panels needed
    }

    override fun getCodeSample(settingsType: SettingsType): String = """
        module Bank {
          var balances: str -> int

          action transfer(from: str, to: str, amount: int): bool = all {
            balances.get(from) >= amount,
            balances' = balances.setBy(from, (b) => b - amount)
                                .setBy(to, (b) => b + amount),
          }
        }
    """.trimIndent()
}
