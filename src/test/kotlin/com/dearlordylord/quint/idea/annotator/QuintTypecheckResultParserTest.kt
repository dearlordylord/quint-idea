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
        assertTrue(result.modules.isEmpty())
        assertTrue(result.types.isEmpty())
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

        // Verify modules parsing
        assertEquals(1, result.modules.size)
        val module = result.modules[0]
        assertEquals("test", module.name)
        assertEquals(4, module.id)
        assertEquals(1, module.declarations.size)
        assertEquals("x", module.declarations[0].name)
        assertEquals("def", module.declarations[0].kind)
        assertEquals("val", module.declarations[0].qualifier)

        // Verify types parsing
        assertEquals(2, result.types.size)
        val type2 = result.types["2"]!!
        assertEquals("str", type2.type.kind)
        val type3 = result.types["3"]!!
        assertEquals("str", type3.type.kind)
    }

    @Test
    fun testParseSuccessfulTypecheckOutput() {
        // Real output from successful quint typecheck
        val json = """{"stage":"typechecking","warnings":[],"modules":[],"table":{},"types":{},"effects":{},"errors":[]}"""

        val result = QuintTypecheckResultParser.parse(json)
        assertEquals("typechecking", result.stage)
        assertTrue(result.errors.isEmpty())
        assertTrue(result.warnings.isEmpty())
        assertTrue(result.modules.isEmpty())
        assertTrue(result.types.isEmpty())
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
        assertTrue(result.modules.isEmpty())
        assertTrue(result.types.isEmpty())
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

    @Test
    fun testParseRecordType() {
        val json = """
            {
              "stage": "typechecking",
              "errors": [],
              "modules": [
                {
                  "id": 10,
                  "name": "mymod",
                  "declarations": [
                    {
                      "id": 5,
                      "kind": "def",
                      "name": "result",
                      "qualifier": "val"
                    }
                  ]
                }
              ],
              "types": {
                "5": {
                  "typeVariables": [],
                  "rowVariables": [],
                  "type": {
                    "kind": "rec",
                    "fields": {
                      "kind": "row",
                      "fields": [
                        {
                          "fieldName": "success",
                          "fieldType": { "kind": "bool" }
                        },
                        {
                          "fieldName": "msg",
                          "fieldType": { "kind": "str" }
                        }
                      ],
                      "other": { "kind": "empty" }
                    }
                  }
                }
              }
            }
        """.trimIndent()

        val result = QuintTypecheckResultParser.parse(json)
        assertEquals(1, result.modules.size)
        assertEquals("mymod", result.modules[0].name)
        assertEquals(1, result.modules[0].declarations.size)
        assertEquals("result", result.modules[0].declarations[0].name)
        assertEquals(5, result.modules[0].declarations[0].id)

        val typeScheme = result.types["5"]!!
        assertEquals("rec", typeScheme.type.kind)
        val fields = typeScheme.type.fields!!
        assertEquals("row", fields.kind)
        assertEquals(2, fields.fields!!.size)
        assertEquals("success", fields.fields!![0].fieldName)
        assertEquals("bool", fields.fields!![0].fieldType.kind)
        assertEquals("msg", fields.fields!![1].fieldName)
        assertEquals("str", fields.fields!![1].fieldType.kind)
    }

    @Test
    fun testParseOperatorType() {
        val json = """
            {
              "stage": "typechecking",
              "errors": [],
              "types": {
                "19": {
                  "typeVariables": [],
                  "rowVariables": [],
                  "type": {
                    "kind": "oper",
                    "args": [
                      { "kind": "bool" },
                      { "kind": "int" }
                    ],
                    "res": { "kind": "str" }
                  }
                }
              }
            }
        """.trimIndent()

        val result = QuintTypecheckResultParser.parse(json)
        val typeScheme = result.types["19"]!!
        assertEquals("oper", typeScheme.type.kind)
        assertEquals(2, typeScheme.type.args!!.size)
        assertEquals("bool", typeScheme.type.args!![0].kind)
        assertEquals("int", typeScheme.type.args!![1].kind)
        assertEquals("str", typeScheme.type.res!!.kind)
    }

    @Test
    fun testParseSumType() {
        val json = """
            {
              "stage": "typechecking",
              "errors": [],
              "types": {
                "14": {
                  "typeVariables": [],
                  "rowVariables": [],
                  "type": {
                    "kind": "sum",
                    "fields": {
                      "kind": "row",
                      "fields": [
                        {
                          "fieldName": "Red",
                          "fieldType": { "kind": "tup", "fields": { "kind": "empty" } }
                        },
                        {
                          "fieldName": "Blue",
                          "fieldType": { "kind": "int" }
                        }
                      ],
                      "other": { "kind": "empty" }
                    }
                  }
                }
              }
            }
        """.trimIndent()

        val result = QuintTypecheckResultParser.parse(json)
        val typeScheme = result.types["14"]!!
        assertEquals("sum", typeScheme.type.kind)
        val fields = typeScheme.type.fields!!.fields!!
        assertEquals(2, fields.size)
        assertEquals("Red", fields[0].fieldName)
        assertEquals("tup", fields[0].fieldType.kind)
        assertEquals("Blue", fields[1].fieldName)
        assertEquals("int", fields[1].fieldType.kind)
    }

    @Test
    fun testParseTypeVariables() {
        val json = """
            {
              "stage": "typechecking",
              "errors": [],
              "types": {
                "33": {
                  "typeVariables": ["t_x_30"],
                  "rowVariables": [],
                  "type": {
                    "kind": "oper",
                    "args": [{ "kind": "var", "name": "t_x_30" }],
                    "res": { "kind": "var", "name": "t_x_30" }
                  }
                }
              }
            }
        """.trimIndent()

        val result = QuintTypecheckResultParser.parse(json)
        val typeScheme = result.types["33"]!!
        assertEquals(listOf("t_x_30"), typeScheme.typeVariables)
        assertEquals("oper", typeScheme.type.kind)
        assertEquals("var", typeScheme.type.args!![0].kind)
        assertEquals("t_x_30", typeScheme.type.args!![0].name)
        assertEquals("var", typeScheme.type.res!!.kind)
        assertEquals("t_x_30", typeScheme.type.res!!.name)
    }

    @Test
    fun testParseConstTypeAlias() {
        val json = """
            {
              "stage": "typechecking",
              "errors": [],
              "modules": [
                {
                  "id": 20,
                  "name": "lib",
                  "declarations": [
                    {
                      "id": 19,
                      "kind": "def",
                      "name": "grapple",
                      "qualifier": "puredef",
                      "typeAnnotation": {
                        "kind": "oper",
                        "args": [{ "kind": "bool" }],
                        "res": { "kind": "const", "name": "GrappleResult" }
                      }
                    }
                  ]
                }
              ]
            }
        """.trimIndent()

        val result = QuintTypecheckResultParser.parse(json)
        val decl = result.modules[0].declarations[0]
        assertEquals("grapple", decl.name)
        assertNotNull(decl.typeAnnotation)
        assertEquals("oper", decl.typeAnnotation!!.kind)
        assertEquals("GrappleResult", decl.typeAnnotation!!.res!!.name)
    }

    @Test
    fun testParseCrossFileImportOutput() {
        // Real output structure from cross-file typecheck
        val json = """
            {
              "stage": "typechecking",
              "errors": [],
              "warnings": [],
              "modules": [
                {
                  "id": 20,
                  "name": "lib",
                  "declarations": [
                    { "id": 9, "kind": "typedef", "name": "GrappleResult" },
                    { "id": 19, "kind": "def", "name": "grapple", "qualifier": "puredef" }
                  ]
                },
                {
                  "id": 5,
                  "name": "main",
                  "declarations": [
                    { "id": 1, "kind": "import", "name": "*" },
                    { "id": 4, "kind": "def", "name": "result", "qualifier": "val" }
                  ]
                }
              ],
              "types": {
                "4": {
                  "typeVariables": [],
                  "rowVariables": [],
                  "type": {
                    "kind": "rec",
                    "fields": {
                      "kind": "row",
                      "fields": [
                        { "fieldName": "success", "fieldType": { "kind": "bool" } },
                        { "fieldName": "msg", "fieldType": { "kind": "str" } }
                      ],
                      "other": { "kind": "empty" }
                    }
                  }
                }
              }
            }
        """.trimIndent()

        val result = QuintTypecheckResultParser.parse(json)
        assertEquals(2, result.modules.size)
        assertEquals("lib", result.modules[0].name)
        assertEquals("main", result.modules[1].name)

        // Import declarations should be parsed (kind=import, name=*)
        val importDecl = result.modules[1].declarations[0]
        assertEquals("import", importDecl.kind)

        // val result should have its type in the types map
        val resultDecl = result.modules[1].declarations[1]
        assertEquals("result", resultDecl.name)
        assertEquals(4, resultDecl.id)
        val resultType = result.types["4"]!!
        assertEquals("rec", resultType.type.kind)
    }
}
