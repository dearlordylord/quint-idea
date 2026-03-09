package com.dearlordylord.quint.idea.references

import com.dearlordylord.quint.idea.psi.QuintNamedElement
import com.intellij.testFramework.fixtures.BasePlatformTestCase

class QuintReferenceTest : BasePlatformTestCase() {

    override fun getTestDataPath(): String = "src/test/testData/references"

    private fun resolveAtCaret(): com.intellij.psi.PsiElement? {
        val ref = myFixture.getReferenceAtCaretPosition()
            ?: myFixture.file.findReferenceAt(myFixture.caretOffset)
        return ref?.resolve()
    }

    private fun referenceAtCaret(): com.intellij.psi.PsiReference? {
        return myFixture.getReferenceAtCaretPosition()
            ?: myFixture.file.findReferenceAt(myFixture.caretOffset)
    }

    fun testModuleLevelValResolves() {
        myFixture.configureByText("test.qnt", "module refs {\n  val x = 1\n  val y = <caret>x\n}")
        val resolved = resolveAtCaret()
        assertNotNull("Expected reference to resolve", resolved)
        assertTrue("Expected QuintNamedElement", resolved is QuintNamedElement)
        assertEquals("x", (resolved as QuintNamedElement).name)
    }

    fun testParamResolves() {
        myFixture.configureByText("test.qnt", "module refs {\n  pure def add(a, b) = <caret>a + b\n}")
        val resolved = resolveAtCaret()
        assertNotNull("Expected reference to resolve to parameter", resolved)
        assertTrue("Expected QuintNamedElement", resolved is QuintNamedElement)
        assertEquals("a", (resolved as QuintNamedElement).name)
    }

    fun testLambdaParamResolves() {
        myFixture.configureByText("test.qnt", "module refs {\n  val f = Set(1, 2, 3).map(x => <caret>x + 1)\n}")
        val resolved = resolveAtCaret()
        assertNotNull("Expected reference to resolve to lambda param", resolved)
        assertTrue("Expected QuintNamedElement", resolved is QuintNamedElement)
        assertEquals("x", (resolved as QuintNamedElement).name)
    }

    fun testQualifiedRefResolves() {
        myFixture.configureByText("test.qnt", "module A {\n  val x = 1\n}\nmodule B {\n  val y = <caret>A::x\n}")
        val resolved = resolveAtCaret()
        assertNotNull("Expected qualified reference to resolve", resolved)
        assertTrue("Expected QuintNamedElement", resolved is QuintNamedElement)
        assertEquals("x", (resolved as QuintNamedElement).name)
    }

    fun testUnresolvableReturnsNull() {
        myFixture.configureByText("test.qnt", "module refs {\n  val x = <caret>unknown\n}")
        val resolved = resolveAtCaret()
        assertNull("Expected unresolvable reference to return null", resolved)
    }

    fun testForwardRefResolves() {
        myFixture.configureByText("test.qnt", "module refs {\n  val y = <caret>x\n  val x = 1\n}")
        val resolved = resolveAtCaret()
        assertNotNull("Expected forward reference to resolve", resolved)
        assertTrue("Expected QuintNamedElement", resolved is QuintNamedElement)
        assertEquals("x", (resolved as QuintNamedElement).name)
    }

    fun testLetInRefResolves() {
        myFixture.configureByText("test.qnt", "module refs {\n  val result = val temp = 42 <caret>temp + 1\n}")
        val resolved = resolveAtCaret()
        assertNotNull("Expected let-in reference to resolve", resolved)
        assertTrue("Expected QuintNamedElement", resolved is QuintNamedElement)
        assertEquals("temp", (resolved as QuintNamedElement).name)
    }

    fun testDeclarationSiteHasNoReference() {
        myFixture.configureByText("test.qnt", "module refs {\n  val <caret>x = 1\n}")
        val ref = referenceAtCaret()
        assertNull("Declaration site should not have a reference", ref)
    }
}
