package com.dearlordylord.quint.idea.completion

import com.intellij.testFramework.fixtures.BasePlatformTestCase

class QuintDotCompletionTest : BasePlatformTestCase() {

    private fun completionStrings(code: String): List<String> {
        myFixture.configureByText("test.qnt", code)
        myFixture.completeBasic()
        return myFixture.lookupElementStrings ?: emptyList()
    }

    fun testDotContextShowsBuiltins() {
        val completions = completionStrings("module M { val s = Set(1, 2, 3).<caret> }")
        assertTrue("filter should be in dot completions", "filter" in completions)
        assertTrue("map should be in dot completions", "map" in completions)
        assertTrue("size should be in dot completions", "size" in completions)
        assertTrue("fold should be in dot completions", "fold" in completions)
        assertTrue("head should be in dot completions", "head" in completions)
    }

    fun testDotContextSuppressesKeywords() {
        val completions = completionStrings("module M { val s = Set(1, 2, 3).<caret> }")
        assertFalse("module should not be in dot completions", "module" in completions)
        assertFalse("val should not be in dot completions", "val" in completions)
        assertFalse("if should not be in dot completions", "if" in completions)
        assertFalse("import should not be in dot completions", "import" in completions)
        assertFalse("else should not be in dot completions", "else" in completions)
        assertFalse("match should not be in dot completions", "match" in completions)
    }

    fun testDotContextSuppressesTypeKeywords() {
        val completions = completionStrings("module M { val s = Set(1, 2, 3).<caret> }")
        assertFalse("int should not be in dot completions", "int" in completions)
        assertFalse("str should not be in dot completions", "str" in completions)
        assertFalse("bool should not be in dot completions", "bool" in completions)
    }

    fun testDotContextSuppressesBuiltinValues() {
        val completions = completionStrings("module M { val s = Set(1, 2, 3).<caret> }")
        assertFalse("Nat should not be in dot completions", "Nat" in completions)
        assertFalse("Bool should not be in dot completions", "Bool" in completions)
    }

    fun testDotContextShowsAndOr() {
        val completions = completionStrings("module M { val s = true.<caret> }")
        assertTrue("and should be in dot completions", "and" in completions)
        assertTrue("or should be in dot completions", "or" in completions)
        assertTrue("iff should be in dot completions", "iff" in completions)
        assertTrue("implies should be in dot completions", "implies" in completions)
    }

    fun testDotContextShowsUserDefs() {
        val completions = completionStrings(
            "module M { def foo(x) = x + 1\n  val s = 1.<caret> }"
        )
        assertTrue("user-defined def foo should be in dot completions", "foo" in completions)
    }

    fun testDotContextHidesUserVals() {
        val completions = completionStrings(
            "module M { val bar = 1\n  val s = 1.<caret> }"
        )
        assertFalse("user-defined val bar should not be in dot completions", "bar" in completions)
    }

    fun testNonDotShowsKeywords() {
        val completions = completionStrings("module M { <caret> }")
        assertTrue("val should be in non-dot completions", "val" in completions)
        assertTrue("def should be in non-dot completions", "def" in completions)
        assertTrue("import should be in non-dot completions", "import" in completions)
    }

    fun testNonDotShowsBuiltinValues() {
        val completions = completionStrings("module M { val x = <caret> }")
        assertTrue("Nat should be in non-dot completions", "Nat" in completions)
    }
}
