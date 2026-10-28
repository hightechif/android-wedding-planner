package app.weddingplanner.core.sheets.local

import app.weddingplanner.core.sheets.A1Range
import app.weddingplanner.core.sheets.CellValue
import app.weddingplanner.core.sheets.CellWrite
import app.weddingplanner.core.sheets.SheetInfo
import app.weddingplanner.core.sheets.SheetRow
import app.weddingplanner.core.sheets.SheetsException
import app.weddingplanner.core.sheets.SheetsGateway
import app.weddingplanner.core.sheets.SpreadsheetMeta
import app.weddingplanner.core.sheets.ValueRange
import app.weddingplanner.core.sheets.ValueRender
import app.weddingplanner.core.sheets.layout.CURRENT_SCHEMA_VERSION
import app.weddingplanner.core.sheets.layout.SCHEMA_VERSION_KEY
import app.weddingplanner.core.sheets.layout.Tabs
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap

/**
 * An in-memory spreadsheet loaded from an .xlsx export (the emulator's local data).
 * It answers the same calls as Google: values are the cached results, formulas aren't evaluated.
 * Writes (the M0 migration, row IDs) are saved to [store] and replayed on the next start, until the
 * xlsx itself changes ([sourceHash]).
 */
class LocalSheetsGateway(
    val spreadsheetId: String,
    private val title: String,
    tabs: List<LocalTab>,
    private val store: LocalChangeStore? = null,
    private val sourceHash: String = "",
) : SheetsGateway {

    class LocalTab(val sheetId: Long, val title: String, val hidden: Boolean) {
        val values = ConcurrentHashMap<Pair<Int, Char>, CellValue>()
        val formulas = ConcurrentHashMap<Pair<Int, Char>, String>()
        val protectedDescriptions: MutableList<String> = java.util.Collections.synchronizedList(mutableListOf())
        val maxRow: Int get() = (values.keys + formulas.keys).maxOfOrNull { it.first } ?: 0
    }

    private val tabs = tabs.associateBy { it.title }.toMutableMap()
    private val metadata = ConcurrentHashMap<String, String>()
    private var changes = LocalChanges(sourceHash)

    init {
        // An .xlsx export can't carry developer metadata. An `_APP_CONFIG` tab means the sheet it came
        // from was already migrated (M0), so restore the schema version it must have had.
        if (Tabs.APP_CONFIG in this.tabs) metadata[SCHEMA_VERSION_KEY] = CURRENT_SCHEMA_VERSION.toString()
        restore()
    }

    override suspend fun getSpreadsheet(spreadsheetId: String): SpreadsheetMeta {
        check(spreadsheetId)
        return SpreadsheetMeta(
            spreadsheetId = this.spreadsheetId,
            title = title,
            sheets = tabs.values.map {
                SheetInfo(it.sheetId, it.title, it.hidden, maxOf(it.maxRow, 1000), 26, it.protectedDescriptions.toList())
            },
            developerMetadata = metadata.toMap(),
        )
    }

    override suspend fun batchGet(spreadsheetId: String, ranges: List<String>, render: ValueRender): List<ValueRange> {
        check(spreadsheetId)
        return ranges.map { text ->
            val r = A1Range.parse(text)
            val tab = tabs[r.tab] ?: throw SheetsException(400, "Unable to parse range: $text")
            val last = minOf(r.lastRow ?: tab.maxRow, tab.maxRow)
            val rows = (r.firstRow..last).map { row ->
                SheetRow(
                    (r.firstColumn..r.lastColumn).map { col ->
                        val formula = tab.formulas[row to col]
                        if (render == ValueRender.FORMULA && formula != null) CellValue.Formula(formula)
                        else tab.values[row to col] ?: CellValue.Empty
                    }.dropLastWhile { it.isBlank },
                )
            }.dropLastWhile { it.cells.isEmpty() }
            ValueRange(r.copy(lastRow = r.firstRow + rows.size - 1), rows)
        }
    }

    override suspend fun writeValues(spreadsheetId: String, writes: List<CellWrite>) {
        check(spreadsheetId)
        writes.forEach { w ->
            val r = A1Range.parse(w.range)
            val tab = tabs[r.tab] ?: throw SheetsException(400, "Unable to parse range: ${w.range}")
            val key = r.firstRow to r.firstColumn
            tab.formulas.remove(key)
            if (w.value.isBlank) tab.values.remove(key) else tab.values[key] = w.value
            changes.cells["${r.tab}!${r.firstColumn}${r.firstRow}"] = StoredCell.of(r.tab, r.firstRow, r.firstColumn, w.value)
        }
        persist()
    }

    override suspend fun batchUpdate(spreadsheetId: String, requests: List<JsonObject>) {
        check(spreadsheetId)
        requests.forEach { req ->
            val (type, body) = req.entries.single()
            val o = body.jsonObject
            when (type) {
                "createDeveloperMetadata" -> {
                    val md = o.getValue("developerMetadata").jsonObject
                    val key = md.getValue("metadataKey").jsonPrimitive.content
                    metadata[key] = md.getValue("metadataValue").jsonPrimitive.content
                    changes.metadata[key] = metadata.getValue(key)
                }
                "addProtectedRange" -> {
                    val pr = o.getValue("protectedRange").jsonObject
                    val id = pr.getValue("range").jsonObject.getValue("sheetId").jsonPrimitive.long
                    val tab = tabs.values.first { it.sheetId == id }
                    val description = pr.getValue("description").jsonPrimitive.content
                    tab.protectedDescriptions += description
                    changes.protections += StoredProtection(tab.title, description)
                }
                "addSheet" -> {
                    val props = o.getValue("properties").jsonObject
                    val t = props.getValue("title").jsonPrimitive.content
                    val hidden = props["hidden"]?.jsonPrimitive?.boolean ?: false
                    tabs.getOrPut(t) { LocalTab(nextSheetId(), t, hidden) }
                    changes.addedTabs += StoredTab(t, hidden)
                }
                // Column visibility has no effect on reads.
                "updateDimensionProperties" -> Unit
                else -> throw SheetsException(400, "Local sheet: unsupported request $type")
            }
        }
        persist()
    }

    /** Replays saved changes, unless they were made against a different xlsx. */
    private fun restore() {
        val saved = store?.load()?.let { runCatching { json.decodeFromString<LocalChanges>(it) }.getOrNull() } ?: return
        if (saved.sourceHash != sourceHash) return
        saved.addedTabs.forEach { t -> tabs.getOrPut(t.title) { LocalTab(nextSheetId(), t.title, t.hidden) } }
        metadata.putAll(saved.metadata)
        saved.protections.forEach { p -> tabs[p.tab]?.protectedDescriptions?.add(p.description) }
        saved.cells.values.forEach { c ->
            val tab = tabs[c.tab] ?: return@forEach
            val key = c.row to c.column
            tab.formulas.remove(key)
            val value = c.toCellValue()
            if (value.isBlank) tab.values.remove(key) else tab.values[key] = value
        }
        changes = saved
    }

    @Synchronized
    private fun persist() {
        store?.save(json.encodeToString(changes))
    }

    private fun nextSheetId() = (tabs.values.maxOfOrNull { it.sheetId } ?: 0) + 1

    private fun check(id: String) {
        if (id != spreadsheetId) throw SheetsException(404, "Local sheet: unknown spreadsheet", googleMessage = "Requested entity was not found.")
    }

    companion object {
        private val json = Json { ignoreUnknownKeys = true }

        /** Tabs the app never reads (template help and example). */
        private val SKIP = setOf("INSTRUKSI!", "WEDDING BUDGET (Contoh)")

        fun fromXlsx(
            spreadsheetId: String,
            title: String,
            xlsx: java.io.InputStream,
            store: LocalChangeStore? = null,
        ): LocalSheetsGateway {
            val bytes = xlsx.readBytes()
            val hash = MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }
            val tabs = XlsxReader.read(bytes.inputStream()).filter { it.title !in SKIP }.mapIndexed { i, tab ->
                LocalTab(sheetId = i + 1L, title = tab.title, hidden = tab.hidden).apply {
                    values.putAll(tab.values)
                    formulas.putAll(tab.formulas)
                }
            }
            return LocalSheetsGateway(spreadsheetId, title, tabs, store, hash)
        }
    }
}

/** Where the local sheet's changes are kept between app starts (a file in the app's private storage). */
interface LocalChangeStore {
    fun load(): String?
    fun save(json: String)
}

@Serializable
private data class LocalChanges(
    val sourceHash: String,
    val cells: MutableMap<String, StoredCell> = linkedMapOf(),
    val metadata: MutableMap<String, String> = linkedMapOf(),
    val addedTabs: MutableList<StoredTab> = mutableListOf(),
    val protections: MutableList<StoredProtection> = mutableListOf(),
)

@Serializable
private data class StoredTab(val title: String, val hidden: Boolean)

@Serializable
private data class StoredProtection(val tab: String, val description: String)

@Serializable
private data class StoredCell(val tab: String, val row: Int, val col: String, val type: String, val value: String) {
    val column: Char get() = col.single()

    fun toCellValue(): CellValue = when (type) {
        "n" -> value.toDoubleOrNull()?.let(CellValue::Number) ?: CellValue.Empty
        "b" -> CellValue.Bool(value == "true")
        "f" -> CellValue.Formula(value)
        "s" -> CellValue.Text(value)
        else -> CellValue.Empty
    }

    companion object {
        fun of(tab: String, row: Int, column: Char, v: CellValue): StoredCell {
            val (type, value) = when (v) {
                CellValue.Empty -> "e" to ""
                is CellValue.Text -> "s" to v.v
                is CellValue.Number -> "n" to v.v.toString()
                is CellValue.Bool -> "b" to v.v.toString()
                is CellValue.Formula -> "f" to v.v
            }
            return StoredCell(tab, row, column.toString(), type, value)
        }
    }
}
