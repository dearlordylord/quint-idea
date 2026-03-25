package com.dearlordylord.quint.idea.annotator

import com.google.gson.Gson
import com.google.gson.JsonSyntaxException

data class QuintTypecheckResult(
    val stage: String,
    val errors: List<QuintError>,
    val warnings: List<QuintError>,
    val modules: List<QuintModule> = emptyList(),
    val types: Map<String, QuintTypeScheme> = emptyMap()
)

data class QuintError(
    val explanation: String,
    val locs: List<QuintErrorLocation>
)

data class QuintErrorLocation(
    val source: String,
    val start: QuintPosition,
    val end: QuintPosition
)

data class QuintPosition(
    val line: Int,
    val col: Int,
    val index: Int
)

data class QuintModule(
    val id: Long,
    val name: String,
    val declarations: List<QuintDeclaration>
)

data class QuintDeclaration(
    val id: Long,
    val kind: String,
    val name: String,
    val qualifier: String?,
    val typeAnnotation: QuintTypeNode?
)

data class QuintTypeScheme(
    val typeVariables: List<String>,
    val rowVariables: List<String>,
    val type: QuintTypeNode
)

data class QuintTypeNode(
    val kind: String,
    val name: String?,
    val fields: QuintRowNode?,
    val args: List<QuintTypeNode>?,
    val res: QuintTypeNode?
) {
    companion object {
        val UNKNOWN = QuintTypeNode(kind = "unknown", name = null, fields = null, args = null, res = null)
    }
}

data class QuintRowNode(
    val kind: String,
    val fields: List<QuintFieldNode>?,
    val other: QuintRowNode?
)

data class QuintFieldNode(
    val fieldName: String,
    val fieldType: QuintTypeNode
)

object QuintTypecheckResultParser {
    private val gson = Gson()

    fun parse(json: String): QuintTypecheckResult {
        try {
            val raw = gson.fromJson(json, RawQuintTypecheckResult::class.java)
                ?: throw IllegalArgumentException("Failed to parse JSON: result was null")
            return QuintTypecheckResult(
                stage = raw.stage ?: "",
                errors = raw.errors?.map { it.toQuintError() } ?: emptyList(),
                warnings = raw.warnings?.map { it.toQuintError() } ?: emptyList(),
                modules = raw.modules?.map { it.toQuintModule() } ?: emptyList(),
                types = parseTypes(raw.types)
            )
        } catch (e: JsonSyntaxException) {
            throw IllegalArgumentException("Invalid JSON: ${e.message}", e)
        }
    }

    private fun parseTypes(typesMap: Map<String, RawQuintTypeScheme>?): Map<String, QuintTypeScheme> {
        if (typesMap == null) return emptyMap()
        return typesMap.mapValues { (_, raw) -> raw.toQuintTypeScheme() }
    }

    // Raw classes for lenient parsing (fields may be absent)
    private data class RawQuintTypecheckResult(
        val stage: String?,
        val errors: List<RawQuintError>?,
        val warnings: List<RawQuintError>?,
        val modules: List<RawQuintModule>?,
        val types: Map<String, RawQuintTypeScheme>?
    )

    private data class RawQuintError(
        val explanation: String?,
        val locs: List<RawQuintErrorLocation>?
    ) {
        fun toQuintError() = QuintError(
            explanation = explanation ?: "",
            locs = locs?.map { it.toQuintErrorLocation() } ?: emptyList()
        )
    }

    private data class RawQuintErrorLocation(
        val source: String?,
        val start: RawQuintPosition?,
        val end: RawQuintPosition?
    ) {
        fun toQuintErrorLocation() = QuintErrorLocation(
            source = source ?: "",
            start = start?.toQuintPosition() ?: QuintPosition(0, 0, 0),
            end = end?.toQuintPosition() ?: QuintPosition(0, 0, 0)
        )
    }

    private data class RawQuintPosition(
        val line: Int?,
        val col: Int?,
        val index: Int?
    ) {
        fun toQuintPosition() = QuintPosition(
            line = line ?: 0,
            col = col ?: 0,
            index = index ?: 0
        )
    }

    private data class RawQuintModule(
        val id: Long?,
        val name: String?,
        val declarations: List<RawQuintDeclaration>?
    ) {
        fun toQuintModule() = QuintModule(
            id = id ?: 0,
            name = name ?: "",
            declarations = declarations?.mapNotNull { it.toQuintDeclaration() } ?: emptyList()
        )
    }

    private data class RawQuintDeclaration(
        val id: Long?,
        val kind: String?,
        val name: String?,
        val qualifier: String?,
        val typeAnnotation: RawQuintTypeNode?
    ) {
        fun toQuintDeclaration(): QuintDeclaration? {
            val declName = name ?: return null
            return QuintDeclaration(
                id = id ?: 0,
                kind = kind ?: "",
                name = declName,
                qualifier = qualifier,
                typeAnnotation = typeAnnotation?.toQuintTypeNode()
            )
        }
    }

    private data class RawQuintTypeScheme(
        val typeVariables: List<String>?,
        val rowVariables: List<String>?,
        val type: RawQuintTypeNode?
    ) {
        fun toQuintTypeScheme() = QuintTypeScheme(
            typeVariables = typeVariables ?: emptyList(),
            rowVariables = rowVariables ?: emptyList(),
            type = type?.toQuintTypeNode() ?: QuintTypeNode.UNKNOWN
        )
    }

    private data class RawQuintTypeNode(
        val kind: String?,
        val name: String?,
        val fields: RawQuintRowNode?,
        val args: List<RawQuintTypeNode>?,
        val res: RawQuintTypeNode?
    ) {
        fun toQuintTypeNode(): QuintTypeNode = QuintTypeNode(
            kind = kind ?: "unknown",
            name = name,
            fields = fields?.toQuintRowNode(),
            args = args?.map { it.toQuintTypeNode() },
            res = res?.toQuintTypeNode()
        )
    }

    private data class RawQuintRowNode(
        val kind: String?,
        val fields: List<RawQuintFieldNode>?,
        val other: RawQuintRowNode?
    ) {
        fun toQuintRowNode(): QuintRowNode = QuintRowNode(
            kind = kind ?: "empty",
            fields = fields?.map { it.toQuintFieldNode() },
            other = other?.toQuintRowNode()
        )
    }

    private data class RawQuintFieldNode(
        val fieldName: String?,
        val fieldType: RawQuintTypeNode?
    ) {
        fun toQuintFieldNode(): QuintFieldNode = QuintFieldNode(
            fieldName = fieldName ?: "",
            fieldType = fieldType?.toQuintTypeNode() ?: QuintTypeNode.UNKNOWN
        )
    }
}
