package com.dearlordylord.quint.idea.parser

import com.dearlordylord.quint.idea.QuintLanguage
import com.intellij.testFramework.ParsingTestCase

class QuintParserTest : ParsingTestCase("parser", "qnt", QuintParserDefinition()) {

    override fun getTestDataPath(): String = "src/test/testData"

    override fun skipSpaces(): Boolean = true
    override fun includeRanges(): Boolean = true

    fun testSimple() {
        doTest(true)
    }

    fun testMultimodule() {
        doTest(true)
    }

    fun testOperators() {
        doTest(true)
    }

    fun testTypes() {
        doTest(true)
    }

    /**
     * Parse the file and verify no errors exist in the PSI tree.
     */
    override fun doTest(checkResult: Boolean) {
        super.doTest(checkResult)
        // Additional check: verify no PsiErrorElement in the tree
        ensureNoErrorElements()
    }
}
