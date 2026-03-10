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

    fun testDeclarationSiteHasSelfReference() {
        myFixture.configureByText("test.qnt", "module refs {\n  val <caret>x = 1\n}")
        val ref = referenceAtCaret()
        assertNotNull("Declaration site should have a self-reference for rename", ref)
        val resolved = ref!!.resolve()
        assertTrue("Self-reference should resolve to QuintNamedElement", resolved is QuintNamedElement)
        assertFalse("Self-reference isReferenceTo should return false", ref.isReferenceTo(resolved!!))
    }

    fun testShadowingResolvesToInnerScope() {
        myFixture.configureByText("test.qnt", "module refs {\n  val x = 1\n  val f = Set(1).map(x => <caret>x)\n}")
        val resolved = resolveAtCaret()
        assertNotNull("Expected shadowed reference to resolve", resolved)
        assertTrue("Expected QuintNamedElement", resolved is QuintNamedElement)
        assertEquals("x", (resolved as QuintNamedElement).name)
        // Should resolve to the lambda parameter, not the module-level val
        val resolvedType = resolved.node?.elementType as? org.antlr.intellij.adaptor.lexer.RuleIElementType
        assertNotNull(resolvedType)
        assertEquals("Should resolve to parameter, not operDef",
            com.dearlordylord.quint.idea.parser.QuintParser.RULE_parameter, resolvedType!!.ruleIndex)
    }

    fun testConstResolves() {
        myFixture.configureByText("test.qnt", "module refs {\n  const N: int\n  val x = <caret>N\n}")
        val resolved = resolveAtCaret()
        assertNotNull("Expected const reference to resolve", resolved)
        assertTrue("Expected QuintNamedElement", resolved is QuintNamedElement)
        assertEquals("N", (resolved as QuintNamedElement).name)
    }

    fun testTypeDefResolves() {
        myFixture.configureByText("test.qnt", "module refs {\n  type MyT = int\n  pure def f(x: <caret>MyT): int = x\n}")
        val resolved = resolveAtCaret()
        assertNotNull("Expected type reference to resolve", resolved)
        assertTrue("Expected QuintNamedElement", resolved is QuintNamedElement)
        assertEquals("MyT", (resolved as QuintNamedElement).name)
    }

    fun testInstanceParamResolves() {
        myFixture.configureByText("test.qnt", "module A {\n  const N: int\n}\nmodule B {\n  import A(<caret>N = 3).*\n}")
        val resolved = resolveAtCaret()
        assertNotNull("Expected instance param to resolve to const", resolved)
        assertTrue("Expected QuintNamedElement", resolved is QuintNamedElement)
        assertEquals("N", (resolved as QuintNamedElement).name)
    }
}
