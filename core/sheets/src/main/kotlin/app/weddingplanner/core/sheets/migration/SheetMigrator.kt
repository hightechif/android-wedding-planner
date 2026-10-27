package app.weddingplanner.core.sheets.migration

import app.weddingplanner.core.model.AccountSeed
import app.weddingplanner.core.sheets.CellValue
import app.weddingplanner.core.sheets.CellWrite
import app.weddingplanner.core.sheets.RowIds
import app.weddingplanner.core.sheets.SheetsGateway
import app.weddingplanner.core.sheets.SpreadsheetMeta
import app.weddingplanner.core.sheets.ValueRange
import app.weddingplanner.core.sheets.ValueRender
import app.weddingplanner.core.sheets.a1Cell
import app.weddingplanner.core.sheets.a1Range
import app.weddingplanner.core.sheets.layout.CURRENT_SCHEMA_VERSION
import app.weddingplanner.core.sheets.layout.SCHEMA_VERSION_KEY
import app.weddingplanner.core.sheets.layout.SheetInspector
import app.weddingplanner.core.sheets.layout.TableLayout
import app.weddingplanner.core.sheets.layout.TableLayouts
import app.weddingplanner.core.sheets.layout.Tabs
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import kotlin.random.Random

const val ID_HEADER = "_id"
const val ID_PROTECTION_DESCRIPTION = "wp:_id"
private const val APP_CONFIG_NOTE = "Dikelola oleh aplikasi Wedding Planner. Jangan diubah."

data class IdAssignment(val row: Int, val id: String, val replacesDuplicate: Boolean)

data class TablePlan(
    val layout: TableLayout,
    val sheetId: Long,
    val mismatches: List<String>,
    /** Problems in column A that make writing unsafe (foreign data where IDs should go). */
    val blockers: List<String>,
    val headerNeedsWrite: Boolean,
    val existingIds: Int,
    val dataRows: Int,
    val lastObservedRow: Int,
    val assignments: List<IdAssignment>,
) {
    val isSafe: Boolean get() = mismatches.isEmpty() && blockers.isEmpty()
}

data class MigrationPlan(
    val spreadsheetId: String,
    val title: String,
    val missingTabs: List<String>,
    val tables: List<TablePlan>,
    val schemaVersion: Int?,
    /** sheetId per tab that holds an ID table. */
    val idTabs: Map<String, Long>,
    val unprotectedTabs: List<String>,
    val appConfigExists: Boolean,
    val appConfigRowCount: Int,
    val accountsToAdd: List<AccountSeed>,
    val accountsSkipped: List<AccountSeed>,
) {
    val canApply: Boolean get() = missingTabs.isEmpty() && tables.all { it.isSafe }
    val idsToAssign: Int get() = tables.sumOf { it.assignments.size }

    /** True when there is nothing left to do (the post-migration verification target). */
    val isComplete: Boolean
        get() = canApply && idsToAssign == 0 && tables.none { it.headerNeedsWrite } &&
            (schemaVersion ?: 0) >= CURRENT_SCHEMA_VERSION && unprotectedTabs.isEmpty() &&
            appConfigExists && accountsToAdd.isEmpty()
}

data class MigrationReport(
    val idsAssigned: Map<String, Int>,
    val steps: List<String>,
    /** Cells outside column A whose formula/value changed during the run. Must be empty. */
    val formulaDiffs: List<String>,
    val verification: MigrationPlan,
) {
    val isSuccess: Boolean get() = formulaDiffs.isEmpty() && verification.isComplete
}

/**
 * One-time, idempotent M0 migration (TRD §5.3): `_id` column A on every ID table, hidden and
 * warning-protected; spreadsheet-level `wp.schemaVersion`; hidden `_APP_CONFIG` tab seeded from
 * the profile accounts. Writes only to column A and to the new tab, and proves it by diffing a
 * FORMULA render of every table before and after.
 */
class SheetMigrator(
    private val gateway: SheetsGateway,
    private val random: Random = Random.Default,
) {
    private val inspector = SheetInspector(gateway)

    suspend fun plan(spreadsheetId: String, accounts: List<AccountSeed>): MigrationPlan {
        val meta = gateway.getSpreadsheet(spreadsheetId)
        val missing = Tabs.REQUIRED.filter { meta.sheet(it) == null }
        val layouts = TableLayouts.ID_TABLES.filter { meta.sheet(it.tab) != null }
        val ranges = inspector.readTables(spreadsheetId, layouts)

        val taken = mutableSetOf<String>()
        val tablePlans = layouts.zip(ranges) { layout, range -> planTable(meta, layout, range, taken) }

        val appConfigExists = meta.sheet(Tabs.APP_CONFIG) != null
        val existingEmails = if (appConfigExists) readAppConfigEmails(spreadsheetId) else emptyList()
        val (withEmail, withoutEmail) = accounts.partition { it.email.isNotBlank() }
        val idTabs = layouts.map { it.tab }.distinct().associateWith { meta.sheet(it)!!.sheetId }

        return MigrationPlan(
            spreadsheetId = spreadsheetId,
            title = meta.title,
            missingTabs = missing,
            tables = tablePlans,
            schemaVersion = meta.developerMetadata[SCHEMA_VERSION_KEY]?.toIntOrNull(),
            idTabs = idTabs,
            unprotectedTabs = idTabs.keys.filter { tab ->
                ID_PROTECTION_DESCRIPTION !in meta.sheet(tab)!!.protectedRangeDescriptions
            },
            appConfigExists = appConfigExists,
            appConfigRowCount = existingEmails.size,
            accountsToAdd = withEmail.filter { it.email.trim().lowercase() !in existingEmails },
            accountsSkipped = withoutEmail,
        )
    }

    suspend fun apply(plan: MigrationPlan, onStep: (String) -> Unit = {}): MigrationReport {
        check(plan.canApply) { "Plan has layout mismatches or column-A conflicts; refusing to write." }
        val steps = mutableListOf<String>()
        fun step(text: String) {
            steps += text
            onStep(text)
        }

        val before = formulaSnapshot(plan)
        step("Snapshot rumus sebelum migrasi: ${before.size} sel")

        val requests = buildList {
            if (plan.schemaVersion == null) add(schemaVersionRequest())
            plan.idTabs.values.forEach { add(hideColumnARequest(it)) }
            plan.unprotectedTabs.forEach { add(protectColumnARequest(plan.idTabs.getValue(it))) }
            if (!plan.appConfigExists) add(addAppConfigSheetRequest())
        }
        gateway.batchUpdate(plan.spreadsheetId, requests)
        step("Struktur: ${requests.size} perubahan (metadata, sembunyikan kolom A, proteksi, tab _APP_CONFIG)")

        val writes = buildList {
            plan.tables.forEach { t ->
                if (t.headerNeedsWrite) add(CellWrite(a1Cell(t.layout.tab, 'A', t.layout.headerRow), CellValue.Text(ID_HEADER)))
                t.assignments.forEach { add(CellWrite(a1Cell(t.layout.tab, 'A', it.row), CellValue.Text(it.id))) }
            }
            addAll(appConfigWrites(plan))
        }
        gateway.writeValues(plan.spreadsheetId, writes)
        step("Nilai: ${plan.idsToAssign} ID baru, ${plan.accountsToAdd.size} akun ke _APP_CONFIG")

        val after = formulaSnapshot(plan)
        val diffs = (before.keys + after.keys).filter { before[it].orEmpty() != after[it].orEmpty() }
            .map { "$it: '${before[it].orEmpty()}' → '${after[it].orEmpty()}'" }
        step(if (diffs.isEmpty()) "Rumus tidak berubah ✓" else "PERINGATAN: ${diffs.size} sel di luar kolom A berubah")

        val accounts = plan.accountsToAdd + plan.accountsSkipped
        val verification = plan(plan.spreadsheetId, accounts)
        step(if (verification.isComplete) "Verifikasi ulang: lengkap ✓" else "Verifikasi ulang: belum lengkap")

        return MigrationReport(
            idsAssigned = plan.tables.associate { it.layout.key to it.assignments.size },
            steps = steps,
            formulaDiffs = diffs,
            verification = verification,
        )
    }

    private fun planTable(meta: SpreadsheetMeta, layout: TableLayout, range: ValueRange, taken: MutableSet<String>): TablePlan {
        val mismatches = SheetInspector.validate(layout, range)
        val blockers = mutableListOf<String>()

        val header = range.row(layout.headerRow)['A'].asText().trim()
        if (header.isNotEmpty() && header != ID_HEADER) {
            blockers += "${layout.tab}!A${layout.headerRow} berisi '$header' (seharusnya kosong atau '$ID_HEADER')"
        }

        val lastRow = layout.lastDataRow ?: (range.firstRow + range.rows.size - 1)
        val pending = mutableListOf<Pair<Int, Boolean>>() // row, replacesDuplicate
        var existing = 0
        var dataRows = 0
        for (row in layout.firstDataRow..lastRow) {
            val cells = range.row(row)
            val a = cells['A'].asText().trim()
            val isData = layout.isDataRow(cells)
            if (isData) dataRows++
            when {
                a.isEmpty() -> if (isData) pending += row to false
                RowIds.isValid(a) -> if (taken.add(a)) existing++ else pending += row to true
                else -> blockers += "${layout.tab}!A$row berisi '$a' (bukan ID aplikasi)"
            }
        }
        val assignments = pending.map { (row, dup) ->
            val id = RowIds.generate(layout.idPrefix, taken, random)
            taken += id
            IdAssignment(row, id, dup)
        }
        return TablePlan(
            layout = layout,
            sheetId = meta.sheet(layout.tab)!!.sheetId,
            mismatches = mismatches,
            blockers = blockers,
            headerNeedsWrite = header.isEmpty(),
            existingIds = existing,
            dataRows = dataRows,
            lastObservedRow = lastRow,
            assignments = assignments,
        )
    }

    private suspend fun readAppConfigEmails(spreadsheetId: String): List<String> =
        gateway.batchGet(spreadsheetId, listOf(a1Range(Tabs.APP_CONFIG, 'A', 2, 'A', 200)), ValueRender.UNFORMATTED)
            .single().rows.map { it['A'].asText().trim().lowercase() }.filter { it.isNotEmpty() }

    /** Every cell from column B to the table's last column, keyed by A1 address. */
    private suspend fun formulaSnapshot(plan: MigrationPlan): Map<String, String> {
        val tables = plan.tables
        val ranges = tables.map { a1Range(it.layout.tab, 'B', it.layout.headerRow, it.layout.lastColumn, it.lastObservedRow) }
        val values = gateway.batchGet(plan.spreadsheetId, ranges, ValueRender.FORMULA)
        val snapshot = mutableMapOf<String, String>()
        tables.zip(values) { table, range ->
            range.rows.forEachIndexed { r, row ->
                row.cells.forEachIndexed { c, cell ->
                    val text = cell.asText()
                    if (text.isNotEmpty()) snapshot["${table.layout.tab}!${'B' + c}${range.firstRow + r}"] = text
                }
            }
        }
        return snapshot
    }

    private fun appConfigWrites(plan: MigrationPlan): List<CellWrite> = buildList {
        val tab = Tabs.APP_CONFIG
        if (!plan.appConfigExists) {
            add(CellWrite(a1Cell(tab, 'A', 1), CellValue.Text("email")))
            add(CellWrite(a1Cell(tab, 'B', 1), CellValue.Text("side")))
            add(CellWrite(a1Cell(tab, 'C', 1), CellValue.Text("role")))
            add(CellWrite(a1Cell(tab, 'E', 1), CellValue.Text(APP_CONFIG_NOTE)))
        }
        plan.accountsToAdd.forEachIndexed { i, account ->
            val row = 2 + plan.appConfigRowCount + i
            add(CellWrite(a1Cell(tab, 'A', row), CellValue.Text(account.email.trim().lowercase())))
            add(CellWrite(a1Cell(tab, 'B', row), CellValue.Text(account.side?.name.orEmpty())))
            add(CellWrite(a1Cell(tab, 'C', row), CellValue.Text(account.role.name)))
        }
    }

    private fun schemaVersionRequest(): JsonObject = buildJsonObject {
        putJsonObject("createDeveloperMetadata") {
            putJsonObject("developerMetadata") {
                put("metadataKey", SCHEMA_VERSION_KEY)
                put("metadataValue", CURRENT_SCHEMA_VERSION.toString())
                putJsonObject("location") { put("spreadsheet", true) }
                put("visibility", "DOCUMENT")
            }
        }
    }

    private fun hideColumnARequest(sheetId: Long): JsonObject = buildJsonObject {
        putJsonObject("updateDimensionProperties") {
            putJsonObject("range") {
                put("sheetId", sheetId)
                put("dimension", "COLUMNS")
                put("startIndex", 0)
                put("endIndex", 1)
            }
            putJsonObject("properties") { put("hiddenByUser", true) }
            put("fields", "hiddenByUser")
        }
    }

    private fun protectColumnARequest(sheetId: Long): JsonObject = buildJsonObject {
        putJsonObject("addProtectedRange") {
            putJsonObject("protectedRange") {
                putJsonObject("range") {
                    put("sheetId", sheetId)
                    put("startColumnIndex", 0)
                    put("endColumnIndex", 1)
                }
                put("description", ID_PROTECTION_DESCRIPTION)
                put("warningOnly", true)
            }
        }
    }

    private fun addAppConfigSheetRequest(): JsonObject = buildJsonObject {
        putJsonObject("addSheet") {
            putJsonObject("properties") {
                put("title", Tabs.APP_CONFIG)
                put("hidden", true)
                putJsonObject("gridProperties") {
                    put("rowCount", 50)
                    put("columnCount", 5)
                }
            }
        }
    }
}
