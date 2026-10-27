package app.weddingplanner.core.sheets

/** Zero-based index of a single-letter column (`A` → 0). The app's tables never go past `Z`. */
fun columnIndex(column: Char): Int {
    require(column in 'A'..'Z') { "Unsupported column $column" }
    return column - 'A'
}

fun columnLetter(index: Int): Char {
    require(index in 0..25) { "Unsupported column index $index" }
    return 'A' + index
}

/** Quotes a tab title for A1 notation: `WEDDING BUDGET` → `'WEDDING BUDGET'`. */
fun quoteTab(title: String): String = "'" + title.replace("'", "''") + "'"

fun a1Cell(tab: String, column: Char, row: Int): String = "${quoteTab(tab)}!$column$row"

/** `'Tab'!A18:K40`, or open-ended `'Tab'!A18:K` when [lastRow] is null. */
fun a1Range(tab: String, firstColumn: Char, firstRow: Int, lastColumn: Char, lastRow: Int?): String =
    "${quoteTab(tab)}!$firstColumn$firstRow:$lastColumn${lastRow ?: ""}"

/** Parsed A1 range, as returned by the API in `valueRanges[].range`. */
data class A1Range(
    val tab: String,
    val firstColumn: Char,
    val firstRow: Int,
    val lastColumn: Char,
    val lastRow: Int?,
) {
    companion object {
        private val PATTERN = Regex("""^(?:'((?:[^']|'')+)'|([^!]+))!([A-Z])(\d+)(?::([A-Z])(\d*))?$""")

        fun parse(text: String): A1Range {
            val m = PATTERN.matchEntire(text) ?: throw IllegalArgumentException("Not an A1 range: $text")
            val tab = m.groupValues[1].takeIf { it.isNotEmpty() }?.replace("''", "'") ?: m.groupValues[2]
            val firstCol = m.groupValues[3].single()
            val firstRow = m.groupValues[4].toInt()
            val lastCol = m.groupValues[5].takeIf { it.isNotEmpty() }?.single() ?: firstCol
            val lastRow = when {
                m.groupValues[5].isEmpty() -> firstRow
                m.groupValues[6].isEmpty() -> null
                else -> m.groupValues[6].toInt()
            }
            return A1Range(tab, firstCol, firstRow, lastCol, lastRow)
        }
    }
}
