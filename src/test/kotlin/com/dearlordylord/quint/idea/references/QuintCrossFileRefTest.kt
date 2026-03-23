package com.dearlordylord.quint.idea.references

import com.dearlordylord.quint.idea.psi.QuintNamedElement
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiNamedElement
import com.intellij.testFramework.fixtures.BasePlatformTestCase

class QuintCrossFileRefTest : BasePlatformTestCase() {

    private fun resolveAtCaret(): PsiElement? {
        val ref = myFixture.getReferenceAtCaretPosition()
            ?: myFixture.file.findReferenceAt(myFixture.caretOffset)
        return ref?.resolve()
    }

    fun testCrossFileQualifiedRef() {
        myFixture.addFileToProject("a.qnt", "module A { val x = 1 }")
        myFixture.configureByText("b.qnt",
            "module B { import A from \"./a.qnt\" val y = <caret>A::x }")
        val resolved = resolveAtCaret()
        assertNotNull("Expected cross-file qualified ref to resolve", resolved)
        assertTrue("Expected QuintNamedElement", resolved is QuintNamedElement)
        assertEquals("x", (resolved as PsiNamedElement).name)
    }

    fun testCrossFileAliasedRef() {
        myFixture.addFileToProject("a.qnt", "module A { val x = 1 }")
        myFixture.configureByText("b.qnt",
            "module B { import A as Foo from \"./a.qnt\" val y = <caret>Foo::x }")
        val resolved = resolveAtCaret()
        assertNotNull("Expected cross-file aliased ref to resolve", resolved)
        assertTrue("Expected QuintNamedElement", resolved is QuintNamedElement)
        assertEquals("x", (resolved as PsiNamedElement).name)
    }

    fun testSameFileQualifiedStillWorks() {
        myFixture.configureByText("test.qnt",
            "module A { val x = 1 }\nmodule B { val y = <caret>A::x }")
        val resolved = resolveAtCaret()
        assertNotNull("Expected same-file qualified ref to still resolve", resolved)
        assertTrue("Expected QuintNamedElement", resolved is QuintNamedElement)
        assertEquals("x", (resolved as PsiNamedElement).name)
    }
}
