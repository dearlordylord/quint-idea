package com.dearlordylord.quint.idea.highlighting

import com.dearlordylord.quint.idea.QuintIcons
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.fileTypes.SyntaxHighlighter
import com.intellij.openapi.options.colors.AttributesDescriptor
import com.intellij.openapi.options.colors.ColorDescriptor
import com.intellij.openapi.options.colors.ColorSettingsPage
import javax.swing.Icon

class QuintColorSettingsPage : ColorSettingsPage {

    companion object {
        private val DESCRIPTORS = arrayOf(
            AttributesDescriptor("Keyword", QuintSyntaxHighlighter.KEYWORD),
            AttributesDescriptor("Operator", QuintSyntaxHighlighter.OPERATOR),
            AttributesDescriptor("String", QuintSyntaxHighlighter.STRING),
            AttributesDescriptor("Number", QuintSyntaxHighlighter.NUMBER),
            AttributesDescriptor("Line comment", QuintSyntaxHighlighter.LINE_COMMENT),
            AttributesDescriptor("Block comment", QuintSyntaxHighlighter.BLOCK_COMMENT),
            AttributesDescriptor("Doc comment", QuintSyntaxHighlighter.DOC_COMMENT),
            AttributesDescriptor("Identifier", QuintSyntaxHighlighter.IDENTIFIER),
            AttributesDescriptor("Type/Class name", QuintSyntaxHighlighter.CLASS_NAME),
            AttributesDescriptor("Braces", QuintSyntaxHighlighter.BRACES),
            AttributesDescriptor("Brackets", QuintSyntaxHighlighter.BRACKETS),
            AttributesDescriptor("Parentheses", QuintSyntaxHighlighter.PARENTHESES),
            AttributesDescriptor("Comma", QuintSyntaxHighlighter.COMMA),
            AttributesDescriptor("Semicolon", QuintSyntaxHighlighter.SEMICOLON),
            AttributesDescriptor("Dot", QuintSyntaxHighlighter.DOT),
            AttributesDescriptor("Bad character", QuintSyntaxHighlighter.BAD_CHARACTER),
        )
    }

    override fun getIcon(): Icon = QuintIcons.FILE
    override fun getHighlighter(): SyntaxHighlighter = QuintSyntaxHighlighter()

    override fun getDemoText(): String = """
/// This is a doc comment
module Example {
  // line comment
  /* block comment */

  const N: int
  var counter: int

  type Direction = North | South | East | West

  import Other from "./other"
  export Other as Alias

  pure val initial: int = 0
  pure def add(a, b) = a + b

  val greeting = "hello"
  val flag = true
  val count = 42
  val hex = 0xFF

  action step = {
    counter' = counter + 1
  }

  val check = if (counter > 0) counter - 1 else 0
  val items = Set[int]
  val xs = List[int]

  val result = counter >= 10 and counter <= 100
  val combined = flag implies count != 0
  val eq = count == 42

  assume _ = N > 0

  run test = step

  temporal prop = all {
    counter >= 0,
    counter <= N,
  }

  match direction {
    | North => 0
    | South => 1
    | _ => 2
  }

  val spread = { ...record, x: 1 }
  val pipe = xs.map(x => x + 1)
  val qualified = Mod::name
}
""".trimIndent()

    override fun getAdditionalHighlightingTagToDescriptorMap(): Map<String, TextAttributesKey>? = null
    override fun getAttributeDescriptors(): Array<AttributesDescriptor> = DESCRIPTORS
    override fun getColorDescriptors(): Array<ColorDescriptor> = ColorDescriptor.EMPTY_ARRAY
    override fun getDisplayName(): String = "Quint"
}
