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

    fun testInstantiatedAliasMember() {
        myFixture.configureByText("test.qnt", "module Counter { const N: int pure val step = N } module Main { import Counter(N = 1) as C pure val result = <caret>C::step }")
        assertEquals("step", (resolveAtCaret() as? PsiNamedElement)?.name)
    }

    fun testMatchBinderResolvesInOwnBranch() {
        myFixture.configureByText("test.qnt", "module M { type T = Some(int) | None pure def f(v: T): int = match v { Some(n) => <caret>n | None => 0 } }")
        assertEquals("n", (resolveAtCaret() as? PsiNamedElement)?.name)
    }

    fun testExportedNamespaceSupportsDeepQualification() {
        myFixture.configureByText("test.qnt", "module A { pure val x = 1 } module B { import A export A } module Main { import B pure val result = <caret>B::A::x }")
        assertEquals("x", (resolveAtCaret() as? PsiNamedElement)?.name)
    }

    fun testUnexportedImportedNamesDoNotLeak() {
        myFixture.configureByText("test.qnt", "module A { pure val x = 1 } module B { import A.* } module Main { import B.* pure val result = <caret>x }")
        assertNull(resolveAtCaret())
    }

    fun testExplicitWildcardExportIsVisible() {
        myFixture.configureByText("test.qnt", "module A { pure val x = 1 } module B { import A export A.* } module Main { import B.* pure val result = <caret>x }")
        assertEquals("x", (resolveAtCaret() as? PsiNamedElement)?.name)
    }

    fun testInstanceWildcardImportsMembers() {
        myFixture.configureByText("test.qnt", "module Counter { const N: int pure val step = N } module Main { import Counter(N = 1).* pure val result = <caret>step }")
        assertEquals("step", (resolveAtCaret() as? PsiNamedElement)?.name)
    }

    fun testMatchBinderDoesNotLeakToOtherBranch() {
        myFixture.configureByText("test.qnt", "module M { type T = Some(int) | None pure def f(v: T): int = match v { Some(n) => n | None => <caret>n } }")
        assertNull(resolveAtCaret())
    }

    fun testRenameMatchBinderRespectsShadowing() {
        myFixture.configureByText("test.qnt", "module M { val n = 99 type T = Some(int) | None pure def f(v: T): int = match v { Some(n) => <caret>n | None => n } }")
        myFixture.renameElementAtCaret("value")
        myFixture.checkResult("module M { val n = 99 type T = Some(int) | None pure def f(v: T): int = match v { Some(value) => value | None => n } }")
    }

    fun testRenameInstantiatedMemberPreservesAlias() {
        myFixture.configureByText("test.qnt", "module Counter { const N: int pure val step = N } module Main { import Counter(N = 1) as C pure val result = <caret>C::step }")
        val declaration = resolveAtCaret()!!
        assertEquals(1, myFixture.findUsages(declaration).size)
        myFixture.renameElementAtCaret("next")
        myFixture.checkResult("module Counter { const N: int pure val next = N } module Main { import Counter(N = 1) as C pure val result = C::next }")
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
              import A.* from "./a"
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
              import A.foo from "./a"
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
    fun testSameFileModuleRequiresImportForQualifiedReference() {
        myFixture.configureByText("test.qnt", "module A { pure val x = 1 } module B { pure val result = <caret>A::x }")
        assertNull(resolveAtCaret())
    }

    fun testInstantiatedMemberRenameCanBeUndone() {
        val text = "module Counter { const N: int pure val step = N } module Main { import Counter(N = 1) as C pure val result = C::step }"
        myFixture.configureByText("test.qnt", text.replace("C::step", "<caret>C::step"))
        myFixture.renameElementAtCaret("next")
        myFixture.checkResult(text.replace("val step", "val next").replace("C::step", "C::next"))
        myFixture.performEditorAction(com.intellij.openapi.actionSystem.IdeActions.ACTION_UNDO)
        myFixture.checkResult(text)
    }
}
