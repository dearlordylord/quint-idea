package com.dearlordylord.quint.idea.completion

import org.junit.Assert.*
import org.junit.Test

/**
 * Tests for QuintCompletionContributor's completion data.
 * We test the static data (keywords, builtins) without needing IDE fixtures
 * since the parser definition has TODO stubs that would throw in a full platform test.
 */
class QuintCompletionTest {

    @Test
    fun testKeywordsContainEssentialKeywords() {
        val keywords = QuintCompletionContributor.KEYWORDS
        assertTrue("module", keywords.contains("module"))
        assertTrue("import", keywords.contains("import"))
        assertTrue("export", keywords.contains("export"))
        assertTrue("from", keywords.contains("from"))
        assertTrue("as", keywords.contains("as"))
        assertTrue("const", keywords.contains("const"))
        assertTrue("var", keywords.contains("var"))
        assertTrue("assume", keywords.contains("assume"))
        assertTrue("type", keywords.contains("type"))
        assertTrue("val", keywords.contains("val"))
        assertTrue("def", keywords.contains("def"))
        assertTrue("pure", keywords.contains("pure"))
        assertTrue("action", keywords.contains("action"))
        assertTrue("run", keywords.contains("run"))
        assertTrue("temporal", keywords.contains("temporal"))
        assertTrue("nondet", keywords.contains("nondet"))
        assertTrue("if", keywords.contains("if"))
        assertTrue("else", keywords.contains("else"))
        assertTrue("match", keywords.contains("match"))
        assertTrue("and", keywords.contains("and"))
        assertTrue("or", keywords.contains("or"))
        assertTrue("all", keywords.contains("all"))
        assertTrue("any", keywords.contains("any"))
        assertTrue("iff", keywords.contains("iff"))
        assertTrue("implies", keywords.contains("implies"))
    }

    @Test
    fun testTypeKeywordsPresent() {
        val typeKws = QuintCompletionContributor.TYPE_KEYWORDS
        assertTrue(typeKws.contains("int"))
        assertTrue(typeKws.contains("str"))
        assertTrue(typeKws.contains("bool"))
        assertTrue(typeKws.contains("Set"))
        assertTrue(typeKws.contains("List"))
    }

    @Test
    fun testBuiltinOperatorsContainSetOps() {
        val ops = QuintCompletionContributor.BUILTIN_OPERATORS
        assertTrue(ops.containsKey("union"))
        assertTrue(ops.containsKey("intersect"))
        assertTrue(ops.containsKey("filter"))
        assertTrue(ops.containsKey("map"))
        assertTrue(ops.containsKey("fold"))
        assertTrue(ops.containsKey("size"))
        assertTrue(ops.containsKey("contains"))
        assertTrue(ops.containsKey("forall"))
        assertTrue(ops.containsKey("exists"))
    }

    @Test
    fun testBuiltinOperatorsContainListOps() {
        val ops = QuintCompletionContributor.BUILTIN_OPERATORS
        assertTrue(ops.containsKey("head"))
        assertTrue(ops.containsKey("tail"))
        assertTrue(ops.containsKey("length"))
        assertTrue(ops.containsKey("append"))
        assertTrue(ops.containsKey("concat"))
        assertTrue(ops.containsKey("nth"))
        assertTrue(ops.containsKey("foldl"))
        assertTrue(ops.containsKey("slice"))
        assertTrue(ops.containsKey("select"))
    }

    @Test
    fun testBuiltinOperatorsContainMapOps() {
        val ops = QuintCompletionContributor.BUILTIN_OPERATORS
        assertTrue(ops.containsKey("keys"))
        assertTrue(ops.containsKey("get"))
        assertTrue(ops.containsKey("put"))
        assertTrue(ops.containsKey("setBy"))
    }

    @Test
    fun testBuiltinOperatorsContainTemporalOps() {
        val ops = QuintCompletionContributor.BUILTIN_OPERATORS
        assertTrue(ops.containsKey("always"))
        assertTrue(ops.containsKey("eventually"))
        assertTrue(ops.containsKey("next"))
        assertTrue(ops.containsKey("enabled"))
    }

    @Test
    fun testBuiltinOperatorsContainActionOps() {
        val ops = QuintCompletionContributor.BUILTIN_OPERATORS
        assertTrue(ops.containsKey("assign"))
        assertTrue(ops.containsKey("then"))
        assertTrue(ops.containsKey("expect"))
        assertTrue(ops.containsKey("fail"))
        assertTrue(ops.containsKey("assert"))
    }

    @Test
    fun testBuiltinValuesPresent() {
        val vals = QuintCompletionContributor.BUILTIN_VALUES
        assertTrue(vals.containsKey("Nat"))
        assertTrue(vals.containsKey("Int"))
        assertTrue(vals.containsKey("Bool"))
    }

    @Test
    fun testBuiltinInfoHasCategory() {
        val ops = QuintCompletionContributor.BUILTIN_OPERATORS
        assertEquals("set", ops["union"]?.category)
        assertEquals("logic", ops["forall"]?.category)
        assertEquals("list", ops["head"]?.category)
        assertEquals("map", ops["get"]?.category)
        assertEquals("temporal", ops["always"]?.category)
        assertEquals("action", ops["assign"]?.category)
    }

    @Test
    fun testBuiltinInfoHasSignature() {
        val ops = QuintCompletionContributor.BUILTIN_OPERATORS
        assertNotNull(ops["union"]?.signature)
        assertTrue(ops["union"]!!.signature.isNotBlank())
        assertTrue(ops["filter"]!!.signature.contains("=>"))
    }

    @Test
    fun testNoKeywordDuplicates() {
        val keywords = QuintCompletionContributor.KEYWORDS
        assertEquals(keywords.size, keywords.distinct().size)
    }

    @Test
    fun testNoTypeKeywordDuplicates() {
        val typeKws = QuintCompletionContributor.TYPE_KEYWORDS
        assertEquals(typeKws.size, typeKws.distinct().size)
    }
}
