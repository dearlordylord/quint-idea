package com.dearlordylord.quint.idea.references

import com.dearlordylord.quint.idea.parser.QuintParser
import com.dearlordylord.quint.idea.psi.QuintPsiUtils
import com.intellij.testFramework.fixtures.BasePlatformTestCase

class QuintImportResolverTest : BasePlatformTestCase() {

    // -- T9.1: extractImportInfo tests --

    private fun parseImportMod(code: String): com.intellij.psi.PsiElement? {
        val file = myFixture.configureByText("test.qnt", code)
        val importMods = QuintPsiUtils.findChildrenOfRule(file, QuintParser.RULE_importMod)
        return importMods.firstOrNull()
    }

    fun testExtractWildcardImport() {
        val importMod = parseImportMod("module M { import A.* }")
        assertNotNull("Expected importMod PSI node", importMod)
        val info = QuintImportResolver.extractImportInfo(importMod!!)
        assertNotNull("Expected ImportInfo", info)
        assertEquals("A", info!!.moduleName)
        assertEquals(ImportKind.WILDCARD, info.kind)
        assertNull(info.alias)
        assertNull(info.specificName)
        assertNull(info.fromSource)
    }

    fun testExtractSpecificImport() {
        val importMod = parseImportMod("module M { import A.foo }")
        assertNotNull(importMod)
        val info = QuintImportResolver.extractImportInfo(importMod!!)
        assertNotNull(info)
        assertEquals("A", info!!.moduleName)
        assertEquals(ImportKind.SPECIFIC, info.kind)
        assertEquals("foo", info.specificName)
        assertNull(info.fromSource)
    }

    fun testExtractQualifiedImport() {
        val importMod = parseImportMod("module M { import A }")
        assertNotNull(importMod)
        val info = QuintImportResolver.extractImportInfo(importMod!!)
        assertNotNull(info)
        assertEquals("A", info!!.moduleName)
        assertEquals(ImportKind.QUALIFIED, info.kind)
        assertNull(info.alias)
        assertNull(info.fromSource)
    }

    fun testExtractAliasedImport() {
        val importMod = parseImportMod("module M { import A as B }")
        assertNotNull(importMod)
        val info = QuintImportResolver.extractImportInfo(importMod!!)
        assertNotNull(info)
        assertEquals("A", info!!.moduleName)
        assertEquals(ImportKind.ALIASED, info.kind)
        assertEquals("B", info.alias)
        assertNull(info.fromSource)
    }

    fun testExtractWithFromSource() {
        val importMod = parseImportMod("""module M { import A.* from "./a.qnt" }""")
        assertNotNull(importMod)
        val info = QuintImportResolver.extractImportInfo(importMod!!)
        assertNotNull(info)
        assertEquals("A", info!!.moduleName)
        assertEquals(ImportKind.WILDCARD, info.kind)
        assertEquals("./a.qnt", info.fromSource)
    }

    fun testExtractQualifiedWithFrom() {
        val importMod = parseImportMod("""module M { import A from "./a.qnt" }""")
        assertNotNull(importMod)
        val info = QuintImportResolver.extractImportInfo(importMod!!)
        assertNotNull(info)
        assertEquals("A", info!!.moduleName)
        assertEquals(ImportKind.QUALIFIED, info.kind)
        assertEquals("./a.qnt", info.fromSource)
    }

    // -- T9.3: findModule tests --

    fun testFindModuleSameFile() {
        myFixture.configureByText("test.qnt", "module A { val x = 1 }\nmodule B { val y = 2 }")
        val file = myFixture.file
        val module = QuintImportResolver.findModule("A", null, file)
        assertNotNull("Expected to find module A", module)
        assertEquals("A", QuintPsiUtils.getDeclarationName(module!!))
    }

    fun testFindModuleCrossFile() {
        myFixture.addFileToProject("a.qnt", "module A { val x = 1 }")
        val bFile = myFixture.configureByText("b.qnt", "module B { val y = 2 }")
        val module = QuintImportResolver.findModule("A", "./a.qnt", bFile)
        assertNotNull("Expected to find module A in a.qnt", module)
        assertEquals("A", QuintPsiUtils.getDeclarationName(module!!))
    }

    // -- findImportsInModule test --

    fun testFindImportsInModule() {
        val file = myFixture.configureByText("test.qnt",
            "module M { import A.* import B import C as D }")
        val modules = QuintPsiUtils.findModules(file)
        assertEquals(1, modules.size)
        val imports = QuintImportResolver.findImportsInModule(modules[0])
        assertEquals(3, imports.size)
        assertEquals(ImportKind.WILDCARD, imports[0].kind)
        assertEquals("A", imports[0].moduleName)
        assertEquals(ImportKind.QUALIFIED, imports[1].kind)
        assertEquals("B", imports[1].moduleName)
        assertEquals(ImportKind.ALIASED, imports[2].kind)
        assertEquals("C", imports[2].moduleName)
        assertEquals("D", imports[2].alias)
    }
}
