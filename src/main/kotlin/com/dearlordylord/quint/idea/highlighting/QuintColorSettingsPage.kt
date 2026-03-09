package com.dearlordylord.quint.idea.highlighting

import com.dearlordylord.quint.idea.QuintIcons
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.fileTypes.SyntaxHighlighter
import com.intellij.openapi.options.colors.AttributesDescriptor
import com.intellij.openapi.options.colors.ColorDescriptor
import com.intellij.openapi.options.colors.ColorSettingsPage
import javax.swing.Icon

class QuintColorSettingsPage : ColorSettingsPage {
    override fun getIcon(): Icon = QuintIcons.FILE
    override fun getHighlighter(): SyntaxHighlighter = QuintSyntaxHighlighter()
    override fun getDemoText(): String = "module Example { val x = 1 }"
    override fun getAdditionalHighlightingTagToDescriptorMap(): Map<String, TextAttributesKey>? = null
    override fun getAttributeDescriptors(): Array<AttributesDescriptor> = emptyArray()
    override fun getColorDescriptors(): Array<ColorDescriptor> = emptyArray()
    override fun getDisplayName(): String = "Quint"
}
