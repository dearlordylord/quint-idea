package com.dearlordylord.quint.idea.annotator

import org.junit.Assert.*
import org.junit.Test

class QuintTypecheckResultParserTest {

    @Test
    fun testParseSuccessResult() {
        val json = """
            {
              "stage": "typechecking",
              "errors": [],
              "warnings": []
            }
        """.trimIndent()

        val result = QuintTypecheckResultParser.parse(json)
        assertEquals("typechecking", result.stage)
        assertTrue(result.errors.isEmpty())
        assertTrue(result.warnings.isEmpty())
    }

    @Test
    fun testParseErrorResult() {
        val json = """
            {
              "stage": "typechecking",
              "errors": [
                {
                  "explanation": "[QNT000] Couldn't unify str and int\nTrying to unify str and int\n",
                  "locs": [
                    {
                      "source": "/tmp/test.qnt",
                      "start": { "line": 1, "col": 6, "index": 20 },
                      "end": { "line": 1, "col": 6, "index": 20 }
                    }
                  ]
                }
              ],
              "warnings": []
            }
        """.trimIndent()

        val result = QuintTypecheckResultParser.parse(json)
        assertEquals("typechecking", result.stage)
        assertEquals(1, result.errors.size)
        assertEquals(0, result.warnings.size)

        val error = result.errors[0]
        assertTrue(error.explanation.contains("Couldn't unify str and int"))
        assertEquals(1, error.locs.size)

        val loc = error.locs[0]
        assertEquals("/tmp/test.qnt", loc.source)
        assertEquals(1, loc.start.line)
        assertEquals(6, loc.start.col)
        assertEquals(20, loc.start.index)
        assertEquals(1, loc.end.line)
        assertEquals(6, loc.end.col)
        assertEquals(20, loc.end.index)
    }

    @Test
    fun testParseMultipleErrors() {
        val json = """
            {
              "stage": "typechecking",
              "errors": [
                {
                  "explanation": "Error 1",
                  "locs": [
                    {
                      "source": "/test.qnt",
                      "start": { "line": 0, "col": 0, "index": 0 },
                      "end": { "line": 0, "col": 5, "index": 5 }
                    }
                  ]
                },
                {
                  "explanation": "Error 2",
                  "locs": [
                    {
                      "source": "/test.qnt",
                      "start": { "line": 3, "col": 2, "index": 50 },
                      "end": { "line": 3, "col": 5, "index": 53 }
                    }
                  ]
                }
              ],
              "warnings": [
                {
                  "explanation": "Warning 1",
                  "locs": []
                }
              ]
            }
        """.trimIndent()

        val result = QuintTypecheckResultParser.parse(json)
        assertEquals(2, result.errors.size)
        assertEquals(1, result.warnings.size)
        assertEquals("Error 1", result.errors[0].explanation)
        assertEquals("Error 2", result.errors[1].explanation)
        assertEquals("Warning 1", result.warnings[0].explanation)
    }

    @Test
    fun testParseRealQuintOutput() {
        // Real output from quint typecheck --out
        val json = """{"stage":"typechecking","warnings":[],"modules":[{"id":4,"name":"test","declarations":[{"id":3,"kind":"def","name":"x","qualifier":"val","expr":{"id":2,"kind":"str","value":"hello"},"typeAnnotation":{"id":1,"kind":"int"}}]}],"table":{"3":{"id":3,"kind":"def","name":"x","qualifier":"val","expr":{"id":2,"kind":"str","value":"hello"},"typeAnnotation":{"id":1,"kind":"int"},"depth":0}},"types":{"2":{"typeVariables":[],"rowVariables":[],"type":{"kind":"str"}},"3":{"typeVariables":[],"rowVariables":[],"type":{"kind":"str"}}},"effects":{"2":{"effect":{"kind":"concrete","components":[]},"effectVariables":[],"entityVariables":[]},"3":{"effectVariables":[],"entityVariables":[],"effect":{"kind":"concrete","components":[]}}},"errors":[{"explanation":"[QNT000] Couldn't unify str and int\nTrying to unify str and int\n","locs":[{"source":"/tmp/test_quint_error.qnt","start":{"line":1,"col":6,"index":20},"end":{"line":1,"col":6,"index":20}}]}]}"""

        val result = QuintTypecheckResultParser.parse(json)
        assertEquals("typechecking", result.stage)
        assertEquals(1, result.errors.size)
        assertEquals(0, result.warnings.size)

        val error = result.errors[0]
        assertTrue(error.explanation.contains("QNT000"))
        assertEquals("/tmp/test_quint_error.qnt", error.locs[0].source)
    }

    @Test
    fun testParseSuccessfulTypecheckOutput() {
        // Real output from successful quint typecheck
        val json = """{"stage":"typechecking","warnings":[],"modules":[],"table":{},"types":{},"effects":{},"errors":[]}"""

        val result = QuintTypecheckResultParser.parse(json)
        assertEquals("typechecking", result.stage)
        assertTrue(result.errors.isEmpty())
        assertTrue(result.warnings.isEmpty())
    }

    @Test
    fun testParseMissingOptionalFields() {
        // Minimal JSON with only stage and errors
        val json = """{"stage":"typechecking","errors":[{"explanation":"test error"}]}"""

        val result = QuintTypecheckResultParser.parse(json)
        assertEquals("typechecking", result.stage)
        assertEquals(1, result.errors.size)
        assertEquals("test error", result.errors[0].explanation)
        assertTrue(result.errors[0].locs.isEmpty())
        assertTrue(result.warnings.isEmpty())
    }

    @Test
    fun testParseEmptyErrorsAndWarnings() {
        val json = """{"stage":"parsing","errors":[],"warnings":[]}"""

        val result = QuintTypecheckResultParser.parse(json)
        assertEquals("parsing", result.stage)
        assertTrue(result.errors.isEmpty())
        assertTrue(result.warnings.isEmpty())
    }

    @Test(expected = IllegalArgumentException::class)
    fun testParseInvalidJson() {
        QuintTypecheckResultParser.parse("not valid json{{{")
    }

    @Test
    fun testParseMultipleLocations() {
        val json = """
            {
              "stage": "typechecking",
              "errors": [
                {
                  "explanation": "Type mismatch",
                  "locs": [
                    {
                      "source": "/a.qnt",
                      "start": { "line": 0, "col": 0, "index": 0 },
                      "end": { "line": 0, "col": 5, "index": 5 }
                    },
                    {
                      "source": "/b.qnt",
                      "start": { "line": 10, "col": 3, "index": 100 },
                      "end": { "line": 10, "col": 8, "index": 105 }
                    }
                  ]
                }
              ]
            }
        """.trimIndent()

        val result = QuintTypecheckResultParser.parse(json)
        assertEquals(1, result.errors.size)
        assertEquals(2, result.errors[0].locs.size)
        assertEquals("/a.qnt", result.errors[0].locs[0].source)
        assertEquals("/b.qnt", result.errors[0].locs[1].source)
        assertEquals(10, result.errors[0].locs[1].start.line)
    }
}
