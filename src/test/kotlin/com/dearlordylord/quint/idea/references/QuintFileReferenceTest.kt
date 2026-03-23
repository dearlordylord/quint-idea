package com.dearlordylord.quint.idea.references

import com.intellij.psi.PsiFile
import com.intellij.testFramework.fixtures.BasePlatformTestCase

class QuintFileReferenceTest : BasePlatformTestCase() {

    private fun findFileReference(): QuintFileReference? {
        var element = myFixture.file.findElementAt(myFixture.caretOffset)
        while (element != null && element !is PsiFile) {
            val ref = element.reference
            if (ref is QuintFileReference) return ref
            element = element.parent
        }
        return null
    }

    fun testFromSourceReferencesFile() {
        myFixture.addFileToProject("a.qnt", "module A {\n  val x = 1\n}")
        myFixture.configureByText("b.qnt",
            """module B { import A.* from "<caret>./a.qnt" }""")
        val ref = findFileReference()
        assertNotNull("Expected file reference on from source string", ref)
        val resolved = ref!!.resolve()
        assertNotNull("Expected reference to resolve to a file", resolved)
        assertTrue("Expected resolved element to be PsiFile", resolved is PsiFile)
        assertEquals("a.qnt", (resolved as PsiFile).name)
    }

    fun testFromSourceQualifiedImport() {
        myFixture.addFileToProject("other.qnt", "module Other {\n  val y = 2\n}")
        myFixture.configureByText("main.qnt",
            """module Main { import Other from "<caret>./other.qnt" }""")
        val ref = findFileReference()
        assertNotNull("Expected file reference", ref)
        val resolved = ref!!.resolve()
        assertNotNull("Expected reference to resolve", resolved)
        assertTrue("Expected PsiFile", resolved is PsiFile)
        assertEquals("other.qnt", (resolved as PsiFile).name)
    }

    fun testFromSourceInSubdirectory() {
        myFixture.addFileToProject("sub/lib.qnt", "module Lib {\n  val z = 3\n}")
        myFixture.configureByText("main.qnt",
            """module Main { import Lib.* from "<caret>./sub/lib.qnt" }""")
        val ref = findFileReference()
        assertNotNull("Expected file reference for subdirectory path", ref)
        val resolved = ref!!.resolve()
        assertNotNull("Expected reference to resolve to subdirectory file", resolved)
        assertTrue("Expected PsiFile", resolved is PsiFile)
        assertEquals("lib.qnt", (resolved as PsiFile).name)
    }

    fun testNonexistentFileReturnsNull() {
        myFixture.configureByText("main.qnt",
            """module Main { import X.* from "<caret>./nonexistent.qnt" }""")
        val ref = findFileReference()
        assertNotNull("Expected file reference even for nonexistent file", ref)
        val resolved = ref!!.resolve()
        assertNull("Expected null for nonexistent file", resolved)
    }
}
