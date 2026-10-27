package app.weddingplanner.core.sheets

import kotlinx.serialization.json.JsonObject

enum class ValueRender(val apiValue: String) {
    UNFORMATTED("UNFORMATTED_VALUE"),
    FORMULA("FORMULA"),
}

data class SheetInfo(
    val sheetId: Long,
    val title: String,
    val hidden: Boolean,
    val rowCount: Int,
    val columnCount: Int,
    val protectedRangeDescriptions: List<String>,
)

data class SpreadsheetMeta(
    val spreadsheetId: String,
    val title: String,
    val sheets: List<SheetInfo>,
    /** Spreadsheet-level developer metadata, key → value. */
    val developerMetadata: Map<String, String>,
) {
    fun sheet(title: String): SheetInfo? = sheets.firstOrNull { it.title == title }
}

/** Rows of one range; [firstRow] is the 1-based row of `rows[0]`. */
data class ValueRange(val range: A1Range, val rows: List<SheetRow>) {
    val firstRow: Int get() = range.firstRow

    /** Row by absolute 1-based row number; rows beyond the returned data are empty. */
    fun row(absoluteRow: Int): SheetRow =
        rows.getOrNull(absoluteRow - firstRow) ?: SheetRow(emptyList())
}

data class CellWrite(val range: String, val value: CellValue)

/**
 * The few Sheets API v4 calls the app needs. Implemented over Retrofit in [RetrofitSheetsGateway]
 * and by an in-memory fake in tests.
 */
interface SheetsGateway {
    suspend fun getSpreadsheet(spreadsheetId: String): SpreadsheetMeta

    suspend fun batchGet(spreadsheetId: String, ranges: List<String>, render: ValueRender): List<ValueRange>

    /** `values:batchUpdate` with `valueInputOption=RAW`. */
    suspend fun writeValues(spreadsheetId: String, writes: List<CellWrite>)

    /** `spreadsheets:batchUpdate` with raw request objects (addSheet, addProtectedRange, …). */
    suspend fun batchUpdate(spreadsheetId: String, requests: List<JsonObject>)
}

/** API failure with the HTTP status, so callers can map 403/404 to user-facing copy. */
class SheetsException(
    val httpCode: Int,
    message: String,
    cause: Throwable? = null,
    /** Google's `error.message` from the response body, if any. */
    val googleMessage: String? = null,
) : Exception(message, cause) {
    val isNoAccess: Boolean get() = httpCode == 403 && !isApiDisabled && !isScopeMissing
    val isNotFound: Boolean get() = httpCode == 404
    val isApiDisabled: Boolean get() = googleMessage?.contains("has not been used in project") == true ||
        googleMessage?.contains("is disabled") == true
    val isScopeMissing: Boolean get() = googleMessage?.contains("insufficient authentication scopes") == true
    /** Excel (.xlsx) files in Drive aren't Google Sheets; the Sheets API rejects them with 400. */
    val isNotGoogleSheet: Boolean get() = googleMessage?.contains("not supported for this document") == true
}
