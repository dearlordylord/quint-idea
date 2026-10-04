package com.dearlordylord.quint.idea.completion

import com.intellij.testFramework.fixtures.BasePlatformTestCase

class QuintRecordIntelligenceTest : BasePlatformTestCase() {
    fun testInlineRecordAnnotationProvidesFields() {
        myFixture.configureByText("main.qnt", "module main { pure def f(r: { count: int, ready: bool }): int = r.<caret> }")
        myFixture.completeBasic()
        val names = myFixture.lookupElementStrings ?: emptyList()
        assertTrue("inline record field must be suggested; got $names", "count" in names)
        assertTrue("ready" in names)
    }
    fun testInlineRecordAnnotationProvidesWithStringFields() {
        myFixture.configureByText("main.qnt", "module main { pure def f(r: { count: int, ready: bool }): int = r.with(\"<caret>\", 2).count }")
        myFixture.completeBasic()
        val names = myFixture.lookupElementStrings ?: emptyList()
        assertTrue("count must be offered for with; got $names", "count" in names)
        assertTrue("ready" in names)
    }

    fun testFunctionValueDoesNotOfferReturnRecordFields() {
        myFixture.configureByText("main.qnt", "module main { pure def make(n: int): { count: int } = { count: n } pure val x = make.<caret> }")
        myFixture.completeBasic()
        assertFalse("a function value is not its result record", "count" in (myFixture.lookupElementStrings ?: emptyList()))
    }
}
