package app.weddingplanner.core.sheets.pull

import app.weddingplanner.core.model.AppUser
import app.weddingplanner.core.model.FinalVendor
import app.weddingplanner.core.model.WeddingEvent
import app.weddingplanner.core.sheets.CellValue
import app.weddingplanner.core.sheets.CellWrite
import app.weddingplanner.core.sheets.SheetsGateway
import app.weddingplanner.core.sheets.ValueRender
import app.weddingplanner.core.sheets.a1Cell
import app.weddingplanner.core.sheets.a1Range
import app.weddingplanner.core.sheets.layout.Tabs
import java.security.MessageDigest

/** Everything one pull read, parsed per module. */
data class PullSnapshot(
    val budget: Parsed<BudgetPull>,
    val events: Parsed<List<WeddingEvent>>,
    val vendors: Parsed<List<FinalVendor>>,
    /** Null when the `_APP_CONFIG` tab doesn't exist yet. */
    val users: List<AppUser>?,
    /** SHA-256 of the raw payload; equal hashes mean nothing changed in the sheet (TRD §5.7). */
    val payloadHash: String,
) {
    val issues: List<String> get() = budget.issues + events.issues + vendors.issues
}

/**
 * Reads every module the app shows in two `values.batchGet` calls (UNFORMATTED, plus FORMULA for
 * the Price column) and parses them. Only column A is ever written, by [writeIds].
 */
class SheetPuller(private val gateway: SheetsGateway) {

    suspend fun pull(spreadsheetId: String, hasAppConfig: Boolean): PullSnapshot {
        val ranges = buildList {
            add(a1Range(Tabs.BUDGET, 'A', 1, 'O', BUDGET_LAST_ROW))
            add(a1Range(Tabs.DASHBOARD, 'A', DashboardParser.RANGE_FIRST_ROW, 'Q', 10))
            add(a1Range(Tabs.VENDOR, 'A', VendorParser.HEADER_ROW, 'I', VendorParser.LAST_ROW))
            if (hasAppConfig) add(a1Range(Tabs.APP_CONFIG, 'A', 1, 'C', 200))
        }
        val values = gateway.batchGet(spreadsheetId, ranges, ValueRender.UNFORMATTED)
        // Ranges start at column A because [SheetRow] indexes cells from A.
        val formulas = gateway.batchGet(
            spreadsheetId,
            listOf(a1Range(Tabs.BUDGET, 'A', 1, 'H', BUDGET_LAST_ROW)),
            ValueRender.FORMULA,
        ).single()

        return PullSnapshot(
            budget = BudgetParser.parse(values[0], formulas),
            events = DashboardParser.parse(values[1]),
            vendors = VendorParser.parse(values[2]),
            users = values.getOrNull(3)?.let(AppConfigParser::parse),
            payloadHash = hash((values + formulas).joinToString("\n") { vr ->
                vr.rows.joinToString("\n") { row -> row.cells.joinToString("\t", transform = CellValue::asText) }
            }),
        )
    }

    /** Writes reconciled IDs into column A of `WEDDING BUDGET` (RAW, one call). */
    suspend fun writeIds(spreadsheetId: String, idsByRow: Map<Int, String>) {
        if (idsByRow.isEmpty()) return
        gateway.writeValues(
            spreadsheetId,
            idsByRow.map { (row, id) -> CellWrite(a1Cell(Tabs.BUDGET, 'A', row), CellValue.Text(id)) },
        )
    }

    private fun hash(text: String): String =
        MessageDigest.getInstance("SHA-256").digest(text.toByteArray()).joinToString("") { "%02x".format(it) }

    companion object {
        /** Well past the template's last row (~106) so inserted rows are still read. */
        const val BUDGET_LAST_ROW = 400
    }
}
