package com.dearlordylord.quint.idea.annotator

import org.junit.Assert.*
import org.junit.Test

/**
 * Tests the full pipeline: JSON parse -> type lookup -> formatted string.
 */
class QuintTypeCacheTest {

    private fun buildTypeMap(result: QuintTypecheckResult): Map<Pair<String, String>, QuintTypeScheme> {
        val map = mutableMapOf<Pair<String, String>, QuintTypeScheme>()
        for (module in result.modules) {
            for (decl in module.declarations) {
                val typeScheme = result.types[decl.id.toString()]
                if (typeScheme != null) {
                    map[module.name to decl.name] = typeScheme
                }
            }
        }
        return map
    }

    @Test
    fun testCrossFileImportTypeLookup() {
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
                },
                "19": {
                  "typeVariables": [],
                  "rowVariables": [],
                  "type": {
                    "kind": "oper",
                    "args": [{ "kind": "bool" }],
                    "res": {
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
            }
        """.trimIndent()

        val result = QuintTypecheckResultParser.parse(json)
        val typeMap = buildTypeMap(result)

        val resultType = typeMap["main" to "result"]!!
        assertEquals("rec", resultType.type.kind)
        assertEquals("{ success: bool, msg: str }", QuintTypeFormatter.formatScheme(resultType))

        val grappleType = typeMap["lib" to "grapple"]!!
        assertEquals("oper", grappleType.type.kind)
        assertEquals("(bool) => { success: bool, msg: str }", QuintTypeFormatter.formatScheme(grappleType))
    }

    @Test
    fun testPolymorphicIdentityFunction() {
        val json = """
            {
              "stage": "typechecking",
              "errors": [],
              "modules": [
                {
                  "id": 34,
                  "name": "types",
                  "declarations": [
                    { "id": 33, "kind": "def", "name": "identity", "qualifier": "puredef" }
                  ]
                }
              ],
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
        assertEquals("x => (x) => x", QuintTypeFormatter.formatScheme(typeScheme))
    }

    @Test
    fun testSumTypeFormatting() {
        val json = """
            {
              "stage": "typechecking",
              "errors": [],
              "modules": [
                {
                  "id": 34,
                  "name": "types",
                  "declarations": [
                    { "id": 19, "kind": "def", "name": "Red", "qualifier": "val" }
                  ]
                }
              ],
              "types": {
                "19": {
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
                          "fieldName": "Green",
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
        val typeScheme = result.types["19"]!!
        assertEquals("Red | Green | Blue(int)", QuintTypeFormatter.formatScheme(typeScheme))
    }

    @Test
    fun testNoTypesAvailable() {
        val json = """{"stage":"typechecking","errors":[],"warnings":[]}"""
        val result = QuintTypecheckResultParser.parse(json)
        assertTrue(result.modules.isEmpty())
        assertTrue(result.types.isEmpty())
    }
}
