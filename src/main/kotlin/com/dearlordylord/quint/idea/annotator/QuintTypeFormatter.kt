package com.dearlordylord.quint.idea.annotator

/**
 * Converts QuintTypeNode (from typecheck JSON) to human-readable type strings.
 */
object QuintTypeFormatter {

    fun format(type: QuintTypeNode): String = formatType(type)

    fun formatScheme(scheme: QuintTypeScheme): String {
        val typeStr = formatType(scheme.type)
        val vars = scheme.typeVariables.map { cleanTypeVar(it) }
        return if (vars.isEmpty()) typeStr else "${vars.joinToString(", ")} => $typeStr"
    }

    private fun formatType(type: QuintTypeNode): String = when (type.kind) {
        "int", "str", "bool" -> type.kind
        "const" -> type.name ?: type.kind
        "var" -> cleanTypeVar(type.name ?: "?")
        "set" -> "Set[${formatArgs(type.args)}]"
        "list" -> "List[${formatArgs(type.args)}]"
        "fun" -> formatFun(type)
        "oper" -> formatOper(type)
        "rec" -> formatRec(type.fields)
        "tup" -> formatTup(type.fields)
        "sum" -> formatSum(type.fields)
        "app" -> formatApp(type)
        else -> type.kind
    }

    private fun formatOper(type: QuintTypeNode): String {
        val args = type.args?.map { formatType(it) } ?: emptyList()
        val res = type.res?.let { formatType(it) } ?: "?"
        return "(${args.joinToString(", ")}) => $res"
    }

    private fun formatFun(type: QuintTypeNode): String {
        val args = type.args?.map { formatType(it) } ?: emptyList()
        val res = type.res?.let { formatType(it) } ?: "?"
        return "${args.firstOrNull() ?: "?"} -> $res"
    }

    private fun formatRec(row: QuintRowNode?): String {
        if (row == null) return "{}"
        val fields = collectFields(row)
        if (fields.isEmpty()) return "{}"
        return "{ ${fields.joinToString(", ") { "${it.fieldName}: ${formatType(it.fieldType)}" }} }"
    }

    private fun formatTup(row: QuintRowNode?): String {
        if (row == null) return "()"
        val fields = collectFields(row)
        if (fields.isEmpty()) return "()"
        // Tuple fields are named "0", "1", etc.
        return "(${fields.joinToString(", ") { formatType(it.fieldType) }})"
    }

    private fun formatSum(row: QuintRowNode?): String {
        if (row == null) return "?"
        val fields = collectFields(row)
        if (fields.isEmpty()) return "?"
        return fields.joinToString(" | ") { variant ->
            val payload = variant.fieldType
            // Empty tuple = no payload
            if (isEmptyTuple(payload)) variant.fieldName
            else "${variant.fieldName}(${formatType(payload)})"
        }
    }

    private fun formatApp(type: QuintTypeNode): String {
        val name = type.name ?: "?"
        val args = type.args?.map { formatType(it) } ?: emptyList()
        return if (args.isEmpty()) name else "$name[${args.joinToString(", ")}]"
    }

    private fun formatArgs(args: List<QuintTypeNode>?): String =
        args?.joinToString(", ") { formatType(it) } ?: "?"

    /**
     * Extract field names from a record type node. Returns null if not a record type.
     */
    fun collectRecordFields(type: QuintTypeNode): List<QuintFieldNode>? {
        if (type.kind != "rec" || type.fields == null) return null
        val fields = collectFields(type.fields)
        return fields.ifEmpty { null }
    }

    private fun collectFields(row: QuintRowNode): List<QuintFieldNode> {
        val result = mutableListOf<QuintFieldNode>()
        var current: QuintRowNode? = row
        while (current != null) {
            if (current.kind == "row") {
                current.fields?.let { result.addAll(it) }
            }
            current = current.other
        }
        return result
    }

    private fun isEmptyTuple(type: QuintTypeNode): Boolean {
        if (type.kind != "tup") return false
        val row = type.fields ?: return true
        return row.kind == "empty" || (row.fields?.isEmpty() ?: true)
    }

    /**
     * Clean up internal type variable names like "t_x_30" to readable names.
     * Uses a simple scheme: first var -> a, second -> b, etc.
     */
    private fun cleanTypeVar(name: String): String {
        // If it looks like an internal name (t_*_N), simplify
        if (name.startsWith("t_") || name.startsWith("r_")) {
            val parts = name.split("_")
            // t_x_30 -> x, t_foo_12 -> foo
            return if (parts.size >= 3) parts[1] else name
        }
        return name
    }
}
