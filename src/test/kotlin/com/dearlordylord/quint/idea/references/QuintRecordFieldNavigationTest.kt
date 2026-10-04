package com.dearlordylord.quint.idea.references

import com.intellij.testFramework.fixtures.BasePlatformTestCase

class QuintRecordFieldNavigationTest : BasePlatformTestCase() {
    fun testImportedTypedefFieldUsesItsDefiningSource() {
        myFixture.addFileToProject("types.qnt", "module Types { type R = { count: int } }")
        myFixture.configureByText("main.qnt", "module main { import Types.* from \"types\" type Unrelated = { count: int } pure def f(r: R): int = r.<caret>count }")
        val target = myFixture.getReferenceAtCaretPosition()?.resolve()
        assertNotNull("field target must be known", target)
        assertEquals("types.qnt", target!!.containingFile.name)
        assertEquals("count", target.text)
    }
    fun testUnknownReceiverDoesNotGuessMatchingField() {
        myFixture.configureByText("main.qnt", "module main { type Unrelated = { count: int } pure def f(r) = r.<caret>count }")
        assertNull("unknown provenance must not select an unrelated field", myFixture.getReferenceAtCaretPosition()?.resolve())
    }
}
