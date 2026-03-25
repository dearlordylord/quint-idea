package com.dearlordylord.quint.idea.annotator

import org.junit.Assert.*
import org.junit.Test

class QuintTypeFormatterTest {

    private fun node(kind: String, name: String? = null, fields: QuintRowNode? = null,
                     args: List<QuintTypeNode>? = null, res: QuintTypeNode? = null) =
        QuintTypeNode(kind = kind, name = name, fields = fields, args = args, res = res)

    private fun row(vararg fieldPairs: Pair<String, QuintTypeNode>, other: QuintRowNode? = null) =
        QuintRowNode(
            kind = "row",
            fields = fieldPairs.map { QuintFieldNode(it.first, it.second) },
            other = other
        )

    private fun emptyRow() = QuintRowNode(kind = "empty", fields = null, other = null)

    @Test
    fun testPrimitiveTypes() {
        assertEquals("int", QuintTypeFormatter.format(node("int")))
        assertEquals("str", QuintTypeFormatter.format(node("str")))
        assertEquals("bool", QuintTypeFormatter.format(node("bool")))
    }

    @Test
    fun testConstType() {
        assertEquals("GrappleResult", QuintTypeFormatter.format(node("const", name = "GrappleResult")))
        assertEquals("Color", QuintTypeFormatter.format(node("const", name = "Color")))
    }

    @Test
    fun testTypeVar() {
        // Internal variable names should be cleaned up
        assertEquals("x", QuintTypeFormatter.format(node("var", name = "t_x_30")))
        assertEquals("foo", QuintTypeFormatter.format(node("var", name = "t_foo_12")))
        // Simple names stay as-is
        assertEquals("a", QuintTypeFormatter.format(node("var", name = "a")))
    }

    @Test
    fun testRecordType() {
        val rec = node("rec", fields = row(
            "success" to node("bool"),
            "msg" to node("str")
        ))
        assertEquals("{ success: bool, msg: str }", QuintTypeFormatter.format(rec))
    }

    @Test
    fun testEmptyRecord() {
        val rec = node("rec", fields = emptyRow())
        assertEquals("{}", QuintTypeFormatter.format(rec))
    }

    @Test
    fun testTupleType() {
        val tup = node("tup", fields = row(
            "0" to node("int"),
            "1" to node("str")
        ))
        assertEquals("(int, str)", QuintTypeFormatter.format(tup))
    }

    @Test
    fun testEmptyTuple() {
        val tup = node("tup", fields = emptyRow())
        assertEquals("()", QuintTypeFormatter.format(tup))
    }

    @Test
    fun testOperatorType() {
        val oper = node("oper",
            args = listOf(node("bool"), node("int")),
            res = node("str")
        )
        assertEquals("(bool, int) => str", QuintTypeFormatter.format(oper))
    }

    @Test
    fun testSingleArgOperator() {
        val oper = node("oper",
            args = listOf(node("bool")),
            res = node("str")
        )
        assertEquals("(bool) => str", QuintTypeFormatter.format(oper))
    }

    @Test
    fun testSumType() {
        val sum = node("sum", fields = row(
            "Red" to node("tup", fields = emptyRow()),
            "Green" to node("tup", fields = emptyRow()),
            "Blue" to node("int")
        ))
        assertEquals("Red | Green | Blue(int)", QuintTypeFormatter.format(sum))
    }

    @Test
    fun testSetType() {
        val set = node("set", args = listOf(node("int")))
        assertEquals("Set[int]", QuintTypeFormatter.format(set))
    }

    @Test
    fun testListType() {
        val list = node("list", args = listOf(node("str")))
        assertEquals("List[str]", QuintTypeFormatter.format(list))
    }

    @Test
    fun testNestedType() {
        // (bool) => { success: bool, msg: str }
        val rec = node("rec", fields = row(
            "success" to node("bool"),
            "msg" to node("str")
        ))
        val oper = node("oper",
            args = listOf(node("bool")),
            res = rec
        )
        assertEquals("(bool) => { success: bool, msg: str }", QuintTypeFormatter.format(oper))
    }

    @Test
    fun testFormatSchemeWithTypeVars() {
        val scheme = QuintTypeScheme(
            typeVariables = listOf("t_x_30"),
            rowVariables = emptyList(),
            type = node("oper",
                args = listOf(node("var", name = "t_x_30")),
                res = node("var", name = "t_x_30")
            )
        )
        assertEquals("x => (x) => x", QuintTypeFormatter.formatScheme(scheme))
    }

    @Test
    fun testFormatSchemeWithoutTypeVars() {
        val scheme = QuintTypeScheme(
            typeVariables = emptyList(),
            rowVariables = emptyList(),
            type = node("int")
        )
        assertEquals("int", QuintTypeFormatter.formatScheme(scheme))
    }
}
