package com.dearlordylord.quint.idea.annotator

import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.google.gson.annotations.SerializedName

data class QuintTypecheckResult(
    val stage: String,
    val errors: List<QuintError>,
    val warnings: List<QuintError>
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

object QuintTypecheckResultParser {
    private val gson = Gson()

    fun parse(json: String): QuintTypecheckResult {
        try {
            val raw = gson.fromJson(json, RawQuintTypecheckResult::class.java)
                ?: throw IllegalArgumentException("Failed to parse JSON: result was null")
            return QuintTypecheckResult(
                stage = raw.stage ?: "",
                errors = raw.errors?.map { it.toQuintError() } ?: emptyList(),
                warnings = raw.warnings?.map { it.toQuintError() } ?: emptyList()
            )
        } catch (e: JsonSyntaxException) {
            throw IllegalArgumentException("Invalid JSON: ${e.message}", e)
        }
    }

    // Raw classes for lenient parsing (fields may be absent)
    private data class RawQuintTypecheckResult(
        val stage: String?,
        val errors: List<RawQuintError>?,
        val warnings: List<RawQuintError>?
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
}
