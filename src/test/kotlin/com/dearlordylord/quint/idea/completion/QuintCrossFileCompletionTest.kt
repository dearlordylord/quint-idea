package com.dearlordylord.quint.idea.completion

import com.intellij.testFramework.fixtures.BasePlatformTestCase

class QuintCrossFileCompletionTest : BasePlatformTestCase() {

    private fun completionStrings(code: String): List<String> {
        myFixture.configureByText("test.qnt", code)
        myFixture.completeBasic()
        return myFixture.lookupElementStrings ?: emptyList()
    }

    fun testWildcardImportCompletion() {
        myFixture.addFileToProject("a.qnt", "module A { val mySpecialVal = 1 }")
        myFixture.configureByText("b.qnt",
            "module B { import A.* from \"./a.qnt\"\n  val y = <caret> }")
        myFixture.completeBasic()
        val names = myFixture.lookupElementStrings ?: emptyList()
        assertTrue("mySpecialVal should appear via wildcard import; got $names", "mySpecialVal" in names)
    }

    fun testSameFileWildcardCompletion() {
        val names = completionStrings(
            "module A { val mySpecialVal = 1 }\nmodule B { import A.* val y = <caret> }")
        assertTrue("mySpecialVal should appear via same-file wildcard import", "mySpecialVal" in names)
    }

    fun testSpecificImportCompletion() {
        myFixture.addFileToProject("a.qnt", "module A { val foo = 1 val bar = 2 }")
        myFixture.configureByText("b.qnt",
            "module B { import A.foo from \"./a.qnt\"\n  val y = <caret> }")
        myFixture.completeBasic()
        val names = myFixture.lookupElementStrings ?: emptyList()
        assertTrue("foo should appear via specific import; got $names", "foo" in names)
    }

    fun testSpecificImportDoesNotShowOtherNames() {
        myFixture.addFileToProject("a.qnt", "module A { val foo = 1 val barUnique = 2 }")
        myFixture.configureByText("b.qnt",
            "module B { import A.foo from \"./a.qnt\"\n  val y = <caret> }")
        myFixture.completeBasic()
        val names = myFixture.lookupElementStrings ?: emptyList()
        assertFalse("barUnique should NOT appear (not imported); got $names", "barUnique" in names)
    }

    fun testSameFileSpecificImportCompletion() {
        val names = completionStrings(
            "module A { val mySpecialVal = 1 val otherVal = 2 }\nmodule B { import A.mySpecialVal val y = <caret> }")
        assertTrue("mySpecialVal should appear via specific import", "mySpecialVal" in names)
    }

    fun testSameFileSpecificImportDoesNotLeakOtherNames() {
        val names = completionStrings(
            "module A { val mySpecialVal = 1 val otherUniqueVal = 2 }\nmodule B { import A.mySpecialVal val y = <caret> }")
        assertFalse("otherUniqueVal should NOT appear (not imported)", "otherUniqueVal" in names)
    }

    fun testWildcardImportShowsMultipleNames() {
        myFixture.addFileToProject("a.qnt", "module A { val alpha = 1 def beta(x) = x }")
        myFixture.configureByText("b.qnt",
            "module B { import A.* from \"./a.qnt\"\n  val y = <caret> }")
        myFixture.completeBasic()
        val names = myFixture.lookupElementStrings ?: emptyList()
        assertTrue("alpha should appear via wildcard import; got $names", "alpha" in names)
        assertTrue("beta should appear via wildcard import; got $names", "beta" in names)
    }

    fun testNoImportMeansNoForeignNames() {
        myFixture.addFileToProject("a.qnt", "module A { val foreignName = 1 }")
        myFixture.configureByText("b.qnt",
            "module B { val y = <caret> }")
        myFixture.completeBasic()
        val names = myFixture.lookupElementStrings ?: emptyList()
        assertFalse("foreignName should NOT appear without any import", "foreignName" in names)
    }
}
