package app.weddingplanner.core.sheets.pull

import app.weddingplanner.core.sheets.CellValue
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.Locale
import kotlin.math.roundToLong

/** Days between the Sheets epoch (1899-12-30) and 1970-01-01. */
private const val SHEETS_EPOCH_OFFSET = 25569L

private val TEXT_DATE_FORMATS = listOf("d MMMM yyyy", "d MMM yyyy", "dd MMMM yyyy", "dd MMM yyyy")
    .map { DateTimeFormatter.ofPattern(it, Locale.ENGLISH) }

internal fun CellValue.textOrNull(): String? = asText().trim().takeIf { it.isNotEmpty() && this !is CellValue.Bool }

internal fun CellValue.doubleOrNull(): Double? = when (this) {
    is CellValue.Number -> v
    is CellValue.Text -> v.trim().toDoubleOrNull()
    else -> null
}

/** Whole rupiah. `UNFORMATTED_VALUE` returns plain numbers, so text is only a fallback. */
internal fun CellValue.rupiahOrNull(): Long? = doubleOrNull()?.roundToLong()

internal fun CellValue.intOrNull(): Int? = doubleOrNull()?.toInt()

/** Dates arrive as serial numbers (days since 1899-12-30); typed text dates are parsed as a fallback. */
internal fun CellValue.dateOrNull(): LocalDate? = when (this) {
    is CellValue.Number -> LocalDate.ofEpochDay(v.toLong() - SHEETS_EPOCH_OFFSET)
    is CellValue.Text -> TEXT_DATE_FORMATS.firstNotNullOfOrNull { format ->
        try {
            LocalDate.parse(v.trim(), format)
        } catch (_: DateTimeParseException) {
            null
        }
    }
    else -> null
}

/** Normalised text for fingerprints: trimmed, single-spaced, lower-case. */
internal fun fingerprintOf(vararg parts: String?): String =
    parts.joinToString("|") { it.orEmpty().trim().replace(Regex("\\s+"), " ").lowercase() }
