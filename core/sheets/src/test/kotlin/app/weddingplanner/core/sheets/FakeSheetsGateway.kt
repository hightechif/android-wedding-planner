package app.weddingplanner.core.sheets

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long

/**
 * In-memory spreadsheet. Formula cells are stored as [CellValue.Formula]; an UNFORMATTED read
 * returns them as `Text("<computed>")`, which is enough for layout and ID logic.
 */
class FakeSheetsGateway(
    val spreadsheetTitle: String = "Test Wedding",
) : SheetsGateway {
    class Tab(val sheetId: Long, val title: String, var hidden: Boolean = false) {
        val cells = mutableMapOf<Pair<Int, Char>, CellValue>()
        val protectedDescriptions = mutableListOf<String>()
        val hiddenColumns = mutableSetOf<Int>()

        fun set(row: Int, column: Char, value: CellValue) {
            if (value.isBlank) cells.remove(row to column) else cells[row to column] = value
        }
        fun text(row: Int, column: Char, text: String) = set(row, column, CellValue.Text(text))
        fun get(row: Int, column: Char): CellValue = cells[row to column] ?: CellValue.Empty
        val maxRow: Int get() = cells.keys.maxOfOrNull { it.first } ?: 0
    }

    val tabs = linkedMapOf<String, Tab>()
    val developerMetadata = mutableMapOf<String, String>()
    var writeCalls = 0
    var batchUpdateCalls = 0
    private var nextSheetId = 1000L

    fun tab(title: String): Tab = tabs.getOrPut(title) { Tab(nextSheetId++, title) }

    override suspend fun getSpreadsheet(spreadsheetId: String) = SpreadsheetMeta(
        spreadsheetId = spreadsheetId,
        title = spreadsheetTitle,
        sheets = tabs.values.map {
            SheetInfo(it.sheetId, it.title, it.hidden, maxOf(it.maxRow, 100), 26, it.protectedDescriptions.toList())
        },
        developerMetadata = developerMetadata.toMap(),
    )

    override suspend fun batchGet(spreadsheetId: String, ranges: List<String>, render: ValueRender): List<ValueRange> =
        ranges.map { text ->
            val r = A1Range.parse(text)
            val tab = tabs[r.tab] ?: error("No tab ${r.tab}")
            val last = r.lastRow ?: tab.maxRow
            val rows = (r.firstRow..last).map { row ->
                SheetRow(
                    (r.firstColumn..r.lastColumn).map { col ->
                        when (val v = tab.get(row, col)) {
                            is CellValue.Formula -> if (render == ValueRender.FORMULA) v else CellValue.Text("<computed>")
                            else -> v
                        }
                    }.dropLastWhile { it.isBlank },
                )
            }.dropLastWhile { it.cells.isEmpty() }
            ValueRange(r.copy(lastRow = last), rows)
        }

    override suspend fun writeValues(spreadsheetId: String, writes: List<CellWrite>) {
        writeCalls++
        writes.forEach { w ->
            val r = A1Range.parse(w.range)
            val tab = tabs[r.tab] ?: error("Write to missing tab ${r.tab}")
            tab.set(r.firstRow, r.firstColumn, w.value)
        }
    }

    override suspend fun batchUpdate(spreadsheetId: String, requests: List<JsonObject>) {
        batchUpdateCalls++
        requests.forEach { req ->
            val (type, body) = req.entries.single()
            val o = body.jsonObject
            when (type) {
                "createDeveloperMetadata" -> {
                    val md = o.getValue("developerMetadata").jsonObject
                    developerMetadata[md.getValue("metadataKey").jsonPrimitive.content] =
                        md.getValue("metadataValue").jsonPrimitive.content
                }
                "updateDimensionProperties" -> {
                    val range = o.getValue("range").jsonObject
                    val tab = bySheetId(range.getValue("sheetId").jsonPrimitive.long)
                    if (o.getValue("properties").jsonObject.getValue("hiddenByUser").jsonPrimitive.boolean) {
                        tab.hiddenColumns += range.getValue("startIndex").jsonPrimitive.long.toInt()
                    }
                }
                "addProtectedRange" -> {
                    val pr = o.getValue("protectedRange").jsonObject
                    val tab = bySheetId(pr.getValue("range").jsonObject.getValue("sheetId").jsonPrimitive.long)
                    tab.protectedDescriptions += pr.getValue("description").jsonPrimitive.content
                }
                "addSheet" -> {
                    val props = o.getValue("properties").jsonObject
                    tab(props.getValue("title").jsonPrimitive.content).hidden =
                        props["hidden"]?.jsonPrimitive?.boolean ?: false
                }
                else -> error("Unsupported request $type")
            }
        }
    }

    private fun bySheetId(id: Long) = tabs.values.single { it.sheetId == id }
}
