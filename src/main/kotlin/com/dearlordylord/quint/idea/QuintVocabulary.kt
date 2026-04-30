package com.dearlordylord.quint.idea

object QuintVocabulary {
    data class BuiltinInfo(val signature: String, val category: String)

    val DOT_CALLABLE_QUALIFIERS = setOf("def", "pure def", "action", "run", "temporal", "nondet")
    val DOT_CALLABLE_KEYWORDS = listOf("and", "or", "iff", "implies")

    val KEYWORDS = listOf(
        "module", "import", "export", "from", "as",
        "const", "var", "assume", "type",
        "val", "def", "pure", "action", "run", "temporal", "nondet",
        "if", "else", "match",
        "and", "or", "all", "any",
        "iff", "implies"
    )

    val TYPE_KEYWORDS = listOf(
        "int", "str", "bool", "Set", "List", "Map", "Rec", "Tup"
    )

    val BUILTIN_OPERATORS = mapOf(
        "eq" to BuiltinInfo("(t, t) => bool", "logic"),
        "neq" to BuiltinInfo("(t, t) => bool", "logic"),
        "not" to BuiltinInfo("(bool) => bool", "logic"),
        "exists" to BuiltinInfo("(Set[a], (a) => bool) => bool", "logic"),
        "forall" to BuiltinInfo("(Set[a], (a) => bool) => bool", "logic"),
        "in" to BuiltinInfo("(a, Set[a]) => bool", "logic"),
        "contains" to BuiltinInfo("(Set[a], a) => bool", "logic"),
        "subseteq" to BuiltinInfo("(Set[a], Set[a]) => bool", "logic"),
        "filter" to BuiltinInfo("(Set[a], (a) => bool) => Set[a]", "logic"),
        "isFinite" to BuiltinInfo("(Set[a]) => bool", "logic"),
        "select" to BuiltinInfo("(List[a], (a) => bool) => List[a]", "logic"),
        "union" to BuiltinInfo("(Set[a], Set[a]) => Set[a]", "set"),
        "intersect" to BuiltinInfo("(Set[a], Set[a]) => Set[a]", "set"),
        "exclude" to BuiltinInfo("(Set[a], Set[a]) => Set[a]", "set"),
        "map" to BuiltinInfo("(Set[a], (a) => b) => Set[b]", "set"),
        "fold" to BuiltinInfo("(Set[a], b, (b, a) => b) => b", "set"),
        "powerset" to BuiltinInfo("(Set[a]) => Set[Set[a]]", "set"),
        "flatten" to BuiltinInfo("(Set[Set[a]]) => Set[a]", "set"),
        "allLists" to BuiltinInfo("(Set[a]) => Set[List[a]]", "set"),
        "allListsUpTo" to BuiltinInfo("(Set[a], int) => Set[List[a]]", "set"),
        "getOnlyElement" to BuiltinInfo("(Set[a]) => a", "set"),
        "chooseSome" to BuiltinInfo("(Set[a]) => a", "set"),
        "oneOf" to BuiltinInfo("(Set[a]) => a", "set"),
        "size" to BuiltinInfo("(Set[a]) => int", "set"),
        "keys" to BuiltinInfo("((a -> b)) => Set[a]", "set"),
        "mapBy" to BuiltinInfo("(Set[a], (a) => b) => (a -> b)", "set"),
        "setToMap" to BuiltinInfo("(Set[(a, b)]) => (a -> b)", "set"),
        "setOfMaps" to BuiltinInfo("(Set[a], Set[b]) => Set[(a -> b)]", "set"),
        "indices" to BuiltinInfo("(List[a]) => Set[int]", "set"),
        "to" to BuiltinInfo("(int, int) => Set[int]", "set"),
        "get" to BuiltinInfo("((a -> b), a) => b", "map"),
        "set" to BuiltinInfo("((a -> b), a, b) => (a -> b)", "map"),
        "setBy" to BuiltinInfo("((a -> b), a, (b) => b) => (a -> b)", "map"),
        "put" to BuiltinInfo("((a -> b), a, b) => (a -> b)", "map"),
        "append" to BuiltinInfo("(List[a], a) => List[a]", "list"),
        "concat" to BuiltinInfo("(List[a], List[a]) => List[a]", "list"),
        "head" to BuiltinInfo("(List[a]) => a", "list"),
        "tail" to BuiltinInfo("(List[a]) => List[a]", "list"),
        "length" to BuiltinInfo("(List[a]) => int", "list"),
        "nth" to BuiltinInfo("(List[a], int) => a", "list"),
        "replaceAt" to BuiltinInfo("(List[a], int, a) => List[a]", "list"),
        "slice" to BuiltinInfo("(List[a], int, int) => List[a]", "list"),
        "range" to BuiltinInfo("(int, int) => List[int]", "list"),
        "foldl" to BuiltinInfo("(List[a], b, (b, a) => b) => b", "list"),
        "iadd" to BuiltinInfo("(int, int) => int", "integer"),
        "isub" to BuiltinInfo("(int, int) => int", "integer"),
        "imul" to BuiltinInfo("(int, int) => int", "integer"),
        "idiv" to BuiltinInfo("(int, int) => int", "integer"),
        "imod" to BuiltinInfo("(int, int) => int", "integer"),
        "ipow" to BuiltinInfo("(int, int) => int", "integer"),
        "iuminus" to BuiltinInfo("(int) => int", "integer"),
        "ilt" to BuiltinInfo("(int, int) => bool", "integer"),
        "igt" to BuiltinInfo("(int, int) => bool", "integer"),
        "ilte" to BuiltinInfo("(int, int) => bool", "integer"),
        "igte" to BuiltinInfo("(int, int) => bool", "integer"),
        "always" to BuiltinInfo("(bool) => bool", "temporal"),
        "eventually" to BuiltinInfo("(bool) => bool", "temporal"),
        "next" to BuiltinInfo("(a) => a", "temporal"),
        "orKeep" to BuiltinInfo("(bool, a) => bool", "temporal"),
        "mustChange" to BuiltinInfo("(bool, a) => bool", "temporal"),
        "enabled" to BuiltinInfo("(bool) => bool", "temporal"),
        "weakFair" to BuiltinInfo("(bool, a) => bool", "temporal"),
        "strongFair" to BuiltinInfo("(bool, a) => bool", "temporal"),
        "assign" to BuiltinInfo("(a, a) => bool", "action"),
        "then" to BuiltinInfo("(bool, bool) => bool", "action"),
        "expect" to BuiltinInfo("(bool, bool) => bool", "action"),
        "reps" to BuiltinInfo("(int, (int) => bool) => bool", "action"),
        "fail" to BuiltinInfo("(bool) => bool", "action"),
        "assert" to BuiltinInfo("(bool) => bool", "action"),
        "with" to BuiltinInfo("(r, str, a) => r", "record"),
        "fieldNames" to BuiltinInfo("(r) => Set[str]", "record"),
    )

    val BUILTIN_VALUES = mapOf(
        "Nat" to BuiltinInfo("Set[int]", "set"),
        "Int" to BuiltinInfo("Set[int]", "set"),
        "Bool" to BuiltinInfo("Set[bool]", "logic"),
    )

    fun isDotCallableQualifier(qualifier: String): Boolean = qualifier in DOT_CALLABLE_QUALIFIERS

    fun builtinInfo(name: String): BuiltinInfo? = BUILTIN_OPERATORS[name] ?: BUILTIN_VALUES[name]
}
