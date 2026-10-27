package app.weddingplanner.core.sheets.layout

import app.weddingplanner.core.sheets.SheetsGateway
import app.weddingplanner.core.sheets.SpreadsheetMeta
import app.weddingplanner.core.sheets.ValueRange
import app.weddingplanner.core.sheets.ValueRender
import app.weddingplanner.core.sheets.a1Range

const val SCHEMA_VERSION_KEY = "wp.schemaVersion"
const val CURRENT_SCHEMA_VERSION = 1

data class TableCheck(val layout: TableLayout, val mismatches: List<String>) {
    val isValid: Boolean get() = mismatches.isEmpty()
}

/** Result of linking a sheet (ONB-2): access, tabs, header layout and migration state. */
data class InspectionReport(
    val meta: SpreadsheetMeta,
    val missingTabs: List<String>,
    val tables: List<TableCheck>,
    val schemaVersion: Int?,
) {
    val title: String get() = meta.title
    val isMigrated: Boolean get() = (schemaVersion ?: 0) >= CURRENT_SCHEMA_VERSION
    val canLink: Boolean get() = missingTabs.isEmpty()
}

/** Reads the tab list and each table's header/sentinel cells and validates them against [TableLayouts]. */
class SheetInspector(private val gateway: SheetsGateway) {

    suspend fun inspect(spreadsheetId: String): InspectionReport {
        val meta = gateway.getSpreadsheet(spreadsheetId)
        val missing = Tabs.REQUIRED.filter { meta.sheet(it) == null }
        val layouts = TableLayouts.ID_TABLES.filter { meta.sheet(it.tab) != null }
        val ranges = readTables(spreadsheetId, layouts)
        return InspectionReport(
            meta = meta,
            missingTabs = missing,
            tables = layouts.zip(ranges) { layout, range -> TableCheck(layout, validate(layout, range)) },
            schemaVersion = meta.developerMetadata[SCHEMA_VERSION_KEY]?.toIntOrNull(),
        )
    }

    suspend fun readTables(spreadsheetId: String, layouts: List<TableLayout>): List<ValueRange> {
        val requested = layouts.map { a1Range(it.tab, 'A', it.readFirstRow, it.lastColumn, it.readLastRow) }
        return gateway.batchGet(spreadsheetId, requested, ValueRender.UNFORMATTED)
    }

    companion object {
        fun validate(layout: TableLayout, range: ValueRange): List<String> = layout.checks.mapNotNull { check ->
            val actual = range.row(check.row)[check.column].asText().trim()
            if (actual.equals(check.expected, ignoreCase = true)) {
                null
            } else {
                "${layout.tab}!${check.column}${check.row}: diharapkan '${check.expected}', ditemukan '$actual'"
            }
        }
    }
}
