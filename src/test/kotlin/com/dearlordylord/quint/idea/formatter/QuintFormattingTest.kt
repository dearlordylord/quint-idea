package com.dearlordylord.quint.idea.formatter

import com.intellij.openapi.command.WriteCommandAction
import com.intellij.psi.codeStyle.CodeStyleManager
import com.intellij.testFramework.fixtures.BasePlatformTestCase

class QuintFormattingTest : BasePlatformTestCase() {

    private fun reformat(code: String): String {
        val file = myFixture.configureByText("test.qnt", code)
        WriteCommandAction.runWriteCommandAction(project) {
            CodeStyleManager.getInstance(project).reformat(file)
        }
        return myFixture.editor.document.text
    }

    fun testReformatModuleBody() {
        val input = """
            |module M {
            |val x = 1
            |val y = 2
            |}
        """.trimMargin()
        val result = reformat(input)
        val lines = result.lines()
        assertTrue("val x should be indented, got: '${lines.getOrNull(1)}'", lines.size > 1 && (lines[1].startsWith("  ") || lines[1].startsWith("    ")))
        assertTrue("val y should be indented", lines.size > 2 && (lines[2].startsWith("  ") || lines[2].startsWith("    ")))
        val closingBrace = lines.last { it.isNotBlank() }
        assertFalse("closing brace should not be indented, got: '$closingBrace'", closingBrace.startsWith(" "))
    }

    fun testReformatNestedBlocks() {
        val input = """
            |module M {
            |val x = and {
            |1 == 1,
            |2 == 2,
            |}
            |}
        """.trimMargin()
        val result = reformat(input)
        val lines = result.lines()
        // val x should be indented once (inside module)
        assertTrue("val x should be indented inside module", lines[1].startsWith("  ") || lines[1].startsWith("    "))
        // 1 == 1 should be indented more than val x
        val valIndent = lines[1].length - lines[1].trimStart().length
        val innerIndent = lines[2].length - lines[2].trimStart().length
        assertTrue("inner content should be more indented than val", innerIndent > valIndent)
    }

    fun testBracesNotIndented() {
        val input = """
            |module M {
            |val x = 1
            |}
        """.trimMargin()
        val result = reformat(input)
        val lines = result.lines()
        assertEquals("opening { should stay on first line", "module M {", lines[0].trimEnd())
        assertFalse("closing } should not be indented", lines.last { it.isNotBlank() }.startsWith(" "))
    }

    fun testReformatRecord() {
        val input = """
            |module M {
            |val r = {
            |name: "alice",
            |age: 30,
            |}
            |}
        """.trimMargin()
        val result = reformat(input)
        val lines = result.lines()
        val valLine = lines.indexOfFirst { it.trimStart().startsWith("val r") }
        val nameLine = lines.indexOfFirst { it.trimStart().startsWith("name") }
        assertTrue("record fields should be indented inside braces", valLine >= 0 && nameLine >= 0)
        val valIndent = lines[valLine].length - lines[valLine].trimStart().length
        val nameIndent = lines[nameLine].length - lines[nameLine].trimStart().length
        assertTrue("record field should be more indented than val", nameIndent > valIndent)
    }

    fun testReformatList() {
        val input = """
            |module M {
            |val l = [
            |1,
            |2,
            |3,
            |]
            |}
        """.trimMargin()
        val result = reformat(input)
        val lines = result.lines()
        val valLine = lines.indexOfFirst { it.trimStart().startsWith("val l") }
        val elemLine = lines.indexOfFirst { it.trim() == "1," }
        assertTrue("list elements should exist", valLine >= 0 && elemLine >= 0)
        val valIndent = lines[valLine].length - lines[valLine].trimStart().length
        val elemIndent = lines[elemLine].length - lines[elemLine].trimStart().length
        assertTrue("list element should be more indented than val", elemIndent > valIndent)
    }

    fun testReformatBlockExpression() {
        val input = """
            |module M {
            |val x = all {
            |a,
            |b,
            |}
            |}
        """.trimMargin()
        val result = reformat(input)
        val lines = result.lines()
        val valLine = lines.indexOfFirst { it.trimStart().startsWith("val x") }
        val aLine = lines.indexOfFirst { it.trim() == "a," }
        assertTrue("block children should exist", valLine >= 0 && aLine >= 0)
        val valIndent = lines[valLine].length - lines[valLine].trimStart().length
        val aIndent = lines[aLine].length - lines[aLine].trimStart().length
        assertTrue("block children should be more indented than val", aIndent > valIndent)
    }

    fun testEnterAfterOpenBrace() {
        myFixture.configureByText("test.qnt", "module M {<caret>}")
        myFixture.type('\n')
        val text = myFixture.editor.document.text
        val lines = text.lines()
        // After typing Enter between { and }, cursor line should be indented
        assertTrue("should have at least 2 lines after Enter", lines.size >= 2)
        val cursorLine = lines[1]
        assertTrue(
            "cursor line should be indented after {",
            cursorLine.isEmpty() || cursorLine.startsWith("  ") || cursorLine.startsWith("    ")
        )
    }

    fun testEnterInModuleBody() {
        myFixture.configureByText("test.qnt", "module M {\n  val x = 1<caret>\n}")
        myFixture.type('\n')
        val caretOffset = myFixture.editor.caretModel.offset
        val text = myFixture.editor.document.text
        val lineNum = myFixture.editor.document.getLineNumber(caretOffset)
        val lineStart = myFixture.editor.document.getLineStartOffset(lineNum)
        val lineEnd = myFixture.editor.document.getLineEndOffset(lineNum)
        val cursorLine = text.substring(lineStart, lineEnd)
        assertTrue(
            "new line inside module body should be indented",
            cursorLine.isEmpty() || cursorLine.startsWith("  ") || cursorLine.startsWith("    ")
        )
    }
}
