package com.dearlordylord.quint.idea.references

import com.dearlordylord.quint.idea.psi.QuintNamedElement
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiNamedElement
import com.intellij.testFramework.fixtures.BasePlatformTestCase

class QuintImportScopeTest : BasePlatformTestCase() {

    private fun resolveAtCaret(): PsiElement? {
        val ref = myFixture.getReferenceAtCaretPosition()
            ?: myFixture.file.findReferenceAt(myFixture.caretOffset)
        return ref?.resolve()
    }

    fun testSameFileWildcardImport() {
        myFixture.configureByText("test.qnt", """
            module A {
              val x = 1
            }
            module B {
              import A.*
              val y = <caret>x
            }
        """.trimIndent())
        val resolved = resolveAtCaret()
        assertNotNull("Wildcard import should make x visible", resolved)
        assertTrue(resolved is QuintNamedElement)
        assertEquals("x", (resolved as PsiNamedElement).name)
    }

    fun testCrossFileWildcardImport() {
        myFixture.addFileToProject("a.qnt", """
            module A {
              val x = 1
            }
        """.trimIndent())
        myFixture.configureByText("b.qnt", """
            module B {
              import A.* from "./a.qnt"
              val y = <caret>x
            }
        """.trimIndent())
        val resolved = resolveAtCaret()
        assertNotNull("Cross-file wildcard import should make x visible", resolved)
        assertTrue(resolved is QuintNamedElement)
        assertEquals("x", (resolved as PsiNamedElement).name)
    }

    fun testSameFileSpecificImport() {
        myFixture.configureByText("test.qnt", """
            module A {
              val x = 1
              val z = 2
            }
            module B {
              import A.x
              val y = <caret>x
            }
        """.trimIndent())
        val resolved = resolveAtCaret()
        assertNotNull("Specific import should make x visible", resolved)
        assertTrue(resolved is QuintNamedElement)
        assertEquals("x", (resolved as PsiNamedElement).name)
    }

    fun testSpecificImportDoesNotLeakOtherNames() {
        myFixture.configureByText("test.qnt", """
            module A {
              val x = 1
              val z = 2
            }
            module B {
              import A.x
              val y = <caret>z
            }
        """.trimIndent())
        val resolved = resolveAtCaret()
        assertNull("Specific import of x should NOT make z visible", resolved)
    }

    fun testCrossFileSpecificImport() {
        myFixture.addFileToProject("a.qnt", """
            module A {
              val foo = 42
            }
        """.trimIndent())
        myFixture.configureByText("b.qnt", """
            module B {
              import A.foo from "./a.qnt"
              val y = <caret>foo
            }
        """.trimIndent())
        val resolved = resolveAtCaret()
        assertNotNull("Cross-file specific import should make foo visible", resolved)
        assertTrue(resolved is QuintNamedElement)
        assertEquals("foo", (resolved as PsiNamedElement).name)
    }

    fun testCrossFileWildcardImportWithoutExtension() {
        myFixture.addFileToProject("a.qnt", """
            module A {
              val x = 1
            }
        """.trimIndent())
        myFixture.configureByText("b.qnt", """
            module B {
              import A.* from "./a"
              val y = <caret>x
            }
        """.trimIndent())
        val resolved = resolveAtCaret()
        assertNotNull("Cross-file wildcard import without .qnt extension should resolve", resolved)
        assertTrue(resolved is QuintNamedElement)
        assertEquals("x", (resolved as PsiNamedElement).name)
    }
}
