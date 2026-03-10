package com.dearlordylord.quint.idea.references

import com.intellij.testFramework.fixtures.BasePlatformTestCase

class QuintRenameTest : BasePlatformTestCase() {

    // --- Rename from usage site ---

    fun testRenameVal() {
        myFixture.configureByText("test.qnt", "module M {\n  val x = 1\n  val y = <caret>x\n}")
        myFixture.renameElementAtCaret("z")
        myFixture.checkResult("module M {\n  val z = 1\n  val y = z\n}")
    }

    fun testRenameDef() {
        myFixture.configureByText("test.qnt", "module M {\n  pure def foo(a) = a + 1\n  val y = <caret>foo(2)\n}")
        myFixture.renameElementAtCaret("bar")
        myFixture.checkResult("module M {\n  pure def bar(a) = a + 1\n  val y = bar(2)\n}")
    }

    fun testRenameConst() {
        myFixture.configureByText("test.qnt", "module M {\n  const N: int\n  val x = <caret>N\n}")
        myFixture.renameElementAtCaret("K")
        myFixture.checkResult("module M {\n  const K: int\n  val x = K\n}")
    }

    fun testRenameType() {
        myFixture.configureByText("test.qnt", "module M {\n  type MyT = int\n  pure def f(x: <caret>MyT): int = x\n}")
        myFixture.renameElementAtCaret("NewT")
        myFixture.checkResult("module M {\n  type NewT = int\n  pure def f(x: NewT): int = x\n}")
    }

    fun testRenameMultipleUsages() {
        myFixture.configureByText("test.qnt", "module M {\n  val x = 1\n  val y = <caret>x\n  val z = x + x\n}")
        myFixture.renameElementAtCaret("w")
        myFixture.checkResult("module M {\n  val w = 1\n  val y = w\n  val z = w + w\n}")
    }

    fun testRenameDoesNotAffectOtherNames() {
        myFixture.configureByText("test.qnt", "module M {\n  val x = 1\n  val y = 2\n  val z = <caret>x + y\n}")
        myFixture.renameElementAtCaret("a")
        myFixture.checkResult("module M {\n  val a = 1\n  val y = 2\n  val z = a + y\n}")
    }

    fun testRenameQualifiedRef() {
        myFixture.configureByText("test.qnt", "module A {\n  val x = 1\n}\nmodule B {\n  val y = <caret>A::x\n}")
        myFixture.renameElementAtCaret("z")
        myFixture.checkResult("module A {\n  val z = 1\n}\nmodule B {\n  val y = A::z\n}")
    }

    // --- Rename from declaration site ---

    fun testRenameParam() {
        myFixture.configureByText("test.qnt", "module M {\n  pure def f(<caret>a) = a\n}")
        myFixture.renameElementAtCaret("b")
        myFixture.checkResult("module M {\n  pure def f(b) = b\n}")
    }

    fun testRenameValFromDeclaration() {
        myFixture.configureByText("test.qnt", "module M {\n  val <caret>x = 1\n  val y = x\n}")
        myFixture.renameElementAtCaret("z")
        myFixture.checkResult("module M {\n  val z = 1\n  val y = z\n}")
    }

    fun testRenameDefFromDeclaration() {
        myFixture.configureByText("test.qnt", "module M {\n  pure def <caret>foo(a) = a + 1\n  val y = foo(2)\n}")
        myFixture.renameElementAtCaret("bar")
        myFixture.checkResult("module M {\n  pure def bar(a) = a + 1\n  val y = bar(2)\n}")
    }

    fun testRenameConstFromDeclaration() {
        myFixture.configureByText("test.qnt", "module M {\n  const <caret>VALS: int\n  val x = VALS\n}")
        myFixture.renameElementAtCaret("ITEMS")
        myFixture.checkResult("module M {\n  const ITEMS: int\n  val x = ITEMS\n}")
    }

    fun testRenameConstSetTypeFromDeclaration() {
        myFixture.configureByText("test.qnt", "module M {\n  const <caret>VALS: Set[int]\n}")
        myFixture.renameElementAtCaret("ITEMS")
        myFixture.checkResult("module M {\n  const ITEMS: Set[int]\n}")
    }

    // --- Rename across instance parameter bindings ---

    fun testRenameConstUpdatesInstanceParam() {
        myFixture.configureByText("test.qnt", "module A {\n  const N: int\n  val x = <caret>N\n}\nmodule B {\n  import A(N = 3).*\n}")
        myFixture.renameElementAtCaret("K")
        myFixture.checkResult("module A {\n  const K: int\n  val x = K\n}\nmodule B {\n  import A(K = 3).*\n}")
    }

    fun testRenameConstFromInstanceParam() {
        myFixture.configureByText("test.qnt", "module A {\n  const N: int\n  val x = N\n}\nmodule B {\n  import A(<caret>N = 3).*\n}")
        myFixture.renameElementAtCaret("K")
        myFixture.checkResult("module A {\n  const K: int\n  val x = K\n}\nmodule B {\n  import A(K = 3).*\n}")
    }
}
