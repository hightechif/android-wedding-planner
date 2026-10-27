package app.weddingplanner.core.sheets

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.doubleOrNull

/** A single cell as read from (or written to) the Sheets API. */
sealed interface CellValue {
    data object Empty : CellValue
    data class Text(val v: String) : CellValue
    data class Number(val v: Double) : CellValue
    data class Bool(val v: Boolean) : CellValue
    data class Formula(val v: String) : CellValue

    val isBlank: Boolean get() = this is Empty || (this is Text && v.isBlank())

    /** Text used for header checks and fingerprints. */
    fun asText(): String = when (this) {
        Empty -> ""
        is Text -> v
        is Number -> if (v % 1.0 == 0.0) v.toLong().toString() else v.toString()
        is Bool -> v.toString().uppercase()
        is Formula -> v
    }

    fun toJson(): JsonElement = when (this) {
        Empty -> JsonPrimitive("")
        is Text -> JsonPrimitive(v)
        is Number -> JsonPrimitive(v)
        is Bool -> JsonPrimitive(v)
        is Formula -> JsonPrimitive(v)
    }

    companion object {
        /**
         * Parses an API value. With `valueRenderOption=UNFORMATTED_VALUE` numbers and booleans
         * arrive as JSON primitives; with `FORMULA` a formula arrives as a string starting with `=`.
         */
        fun fromJson(element: JsonElement, formulaRender: Boolean = false): CellValue {
            if (element is JsonNull || element !is JsonPrimitive) return Empty
            if (element.isString) {
                val s = element.content
                return when {
                    s.isEmpty() -> Empty
                    formulaRender && s.startsWith("=") -> Formula(s)
                    else -> Text(s)
                }
            }
            element.booleanOrNull?.let { return Bool(it) }
            element.doubleOrNull?.let { return Number(it) }
            return Text(element.content)
        }
    }
}

/** A row of cells indexed by column (A = 0). Missing trailing cells read as [CellValue.Empty]. */
@JvmInline
value class SheetRow(val cells: List<CellValue>) {
    operator fun get(column: Char): CellValue = cells.getOrElse(columnIndex(column)) { CellValue.Empty }
}
