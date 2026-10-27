package app.weddingplanner.core.sheets.pull

import app.weddingplanner.core.model.AppUser
import app.weddingplanner.core.model.BudgetItem
import app.weddingplanner.core.model.BudgetSummary
import app.weddingplanner.core.model.BudgetTable
import app.weddingplanner.core.model.EventType
import app.weddingplanner.core.model.FinalVendor
import app.weddingplanner.core.model.Role
import app.weddingplanner.core.model.Side
import app.weddingplanner.core.model.WeddingEvent
import app.weddingplanner.core.sheets.CellValue
import app.weddingplanner.core.sheets.RowIds
import app.weddingplanner.core.sheets.ValueRange
import app.weddingplanner.core.sheets.layout.CellCheck
import app.weddingplanner.core.sheets.layout.TableLayout
import app.weddingplanner.core.sheets.layout.TableLayouts
import app.weddingplanner.core.sheets.layout.Tabs

/** A module's parse result: [value] is null when its layout didn't match ([issues] says why). */
data class Parsed<out T>(val value: T?, val issues: List<String> = emptyList()) {
    val isValid: Boolean get() = value != null
}

/** A sheet row as read, before row identity is reconciled. [id] is column A, if it holds a valid ID. */
data class ObservedRow<out T>(val sheetRow: Int, val id: String?, val fingerprint: String, val value: T)

data class BudgetPull(
    val akad: List<ObservedRow<BudgetItem>>,
    val mahar: List<ObservedRow<BudgetItem>>,
    val summary: BudgetSummary,
    /** Rows the Mahar table moved by when rows were inserted above it (0 = template layout). */
    val offset: Int,
)

private fun ValueRange.cell(row: Int, column: Char): CellValue = row(row)[column]

private fun check(range: ValueRange, tab: String, checks: List<CellCheck>, shift: Int = 0): List<String> =
    checks.mapNotNull { c ->
        val row = c.row + shift
        val actual = range.cell(row, c.column).asText().trim()
        if (actual.equals(c.expected, ignoreCase = true)) null
        else "$tab!${c.column}$row: diharapkan '${c.expected}', ditemukan '$actual'"
    }

/**
 * `WEDDING BUDGET` (docs/05-DATA-SCHEMA.md §5.1–5.3). Reads the whole tab in one range; the
 * Akad table's end is found from the "MAHAR & SESERAHAN" sentinel, so rows inserted in Sheets
 * shift the Mahar table instead of breaking it (TRD §5.2).
 */
object BudgetParser {
    const val SENTINEL = "MAHAR & SESERAHAN"
    private const val TEMPLATE_SENTINEL_ROW = 77

    private val SUMMARY_CHECKS = listOf(
        CellCheck(7, 'B', "BUDGETING NIKAH"),
        CellCheck(6, 'K', "Total Biaya"),
        CellCheck(11, 'K', "Item Lunas"),
    )
    private val ITEM_COUNT = Regex("""(\d+)\s*dari\s*(\d+)""")
    private val FIRST_NUMBER = Regex("""\d+""")

    /**
     * @param values `A1:O…` with UNFORMATTED values.
     * @param formulas `A1:H…` with FORMULA render, to keep Price expressions such as `=1300000+800000`.
     */
    fun parse(values: ValueRange, formulas: ValueRange?): Parsed<BudgetPull> {
        val akad = TableLayouts.BUDGET_AKAD
        val mahar = TableLayouts.BUDGET_MAHAR
        val lastRow = values.firstRow + values.rows.size - 1
        val sentinelRow = (akad.firstDataRow..lastRow).firstOrNull {
            values.cell(it, 'B').asText().trim().equals(SENTINEL, ignoreCase = true)
        } ?: return Parsed(null, listOf("${Tabs.BUDGET}: judul '$SENTINEL' tidak ditemukan di kolom B"))
        val offset = sentinelRow - TEMPLATE_SENTINEL_ROW

        val issues = check(values, Tabs.BUDGET, SUMMARY_CHECKS) +
            check(values, Tabs.BUDGET, akad.checks.filter { it.row == akad.headerRow }) +
            check(values, Tabs.BUDGET, mahar.checks, shift = offset)
        if (issues.isNotEmpty()) return Parsed(null, issues)

        val akadRows = (akad.firstDataRow..requireNotNull(akad.lastDataRow) + offset)
            .mapNotNull { parseItem(values, formulas, it, BudgetTable.AKAD_RESEPSI, akad) }
        val maharRows = ((mahar.firstDataRow + offset)..lastRow)
            .mapNotNull { parseItem(values, formulas, it, BudgetTable.MAHAR_SESERAHAN, mahar) }
        return Parsed(BudgetPull(akadRows, maharRows, parseSummary(values), offset))
    }

    private fun parseItem(
        values: ValueRange,
        formulas: ValueRange?,
        sheetRow: Int,
        table: BudgetTable,
        layout: TableLayout,
    ): ObservedRow<BudgetItem>? {
        val row = values.row(sheetRow)
        if (!layout.isDataRow(row)) return null
        val id = row['A'].textOrNull()?.takeIf { RowIds.isValid(it) }
        val category = row['C'].textOrNull().orEmpty()
        val detail = row['D'].textOrNull().orEmpty()
        val vendor = row['E'].textOrNull()
        val item = BudgetItem(
            rowId = id.orEmpty(),
            table = table,
            sheetRow = sheetRow,
            no = row['B'].intOrNull(),
            categoryRaw = category,
            detail = detail,
            vendorOrPayer = vendor,
            qty = row['F'].doubleOrNull(),
            unit = row['G'].textOrNull(),
            price = row['H'].rupiahOrNull(),
            priceFormula = (formulas?.row(sheetRow)?.get('H') as? CellValue.Formula)?.v,
            total = row['I'].rupiahOrNull() ?: 0,
            paid = row['J'].rupiahOrNull() ?: 0,
            remaining = row['K'].rupiahOrNull(),
            progress = row['L'].doubleOrNull(),
            statusRaw = row['M'].textOrNull(),
            dueDate = row['N'].dateOrNull(),
            notes = row['O'].textOrNull(),
        )
        return ObservedRow(sheetRow, id, fingerprintOf(category, detail, vendor), item)
    }

    private fun parseSummary(v: ValueRange): BudgetSummary {
        fun rp(row: Int, col: Char) = v.cell(row, col).rupiahOrNull() ?: 0
        val itemCount = ITEM_COUNT.find(v.cell(11, 'L').asText())
        return BudgetSummary(
            budgetingNikah = rp(8, 'B'),
            budgetTerpakai = v.cell(6, 'E').doubleOrNull() ?: 0.0,
            totalEstimasi = rp(11, 'B'),
            selisih = rp(14, 'B'),
            totalAkadResepsi = rp(18, 'B'),
            totalMaharSeserahan = rp(20, 'B'),
            totalBiaya = rp(6, 'L'),
            sudahDibayar = rp(7, 'L'),
            sisaPembayaran = rp(8, 'L'),
            progress = v.cell(9, 'L').doubleOrNull() ?: 0.0,
            itemLunas = itemCount?.groupValues?.get(1)?.toInt(),
            itemTotal = itemCount?.groupValues?.get(2)?.toInt(),
            dueIn14Days = FIRST_NUMBER.find(v.cell(12, 'L').asText())?.value?.toInt(),
        )
    }
}

/** `DASHBOARD!A9:Q10`: event titles in row 9, dates in row 10 (§5.8). */
object DashboardParser {
    const val RANGE_FIRST_ROW = 9
    private val EVENT_COLUMNS = listOf(
        Triple(EventType.LAMARAN, 'C', "Lamaran"),
        Triple(EventType.PENGAJIAN_SIRAMAN, 'G', "Pengajian"),
        Triple(EventType.PRE_WEDDING, 'K', "Pre Wedding"),
        Triple(EventType.WEDDING, 'O', "Wedding"),
    )

    fun parse(range: ValueRange): Parsed<List<WeddingEvent>> {
        val issues = EVENT_COLUMNS.mapNotNull { (_, col, label) ->
            val actual = range.cell(9, col).asText()
            if (actual.contains(label, ignoreCase = true)) null
            else "${Tabs.DASHBOARD}!${col}9: diharapkan memuat '$label', ditemukan '${actual.trim()}'"
        }
        if (issues.isNotEmpty()) return Parsed(null, issues)
        return Parsed(EVENT_COLUMNS.mapNotNull { (type, col, _) -> range.cell(10, col).dateOrNull()?.let { WeddingEvent(type, it) } })
    }
}

/** Finalisasi Vendor, `'Perbandingan Vendor'!A5:I14` (§5.7). */
object VendorParser {
    const val HEADER_ROW = 5
    const val LAST_ROW = 14
    private val CHECKS = listOf(
        CellCheck(5, 'B', "Kategori"),
        CellCheck(5, 'D', "Nama Vendor"),
        CellCheck(5, 'F', "Pembayaran"),
        CellCheck(5, 'G', "Pertemuan"),
        CellCheck(5, 'H', "Keterangan Lain"),
    )

    fun parse(range: ValueRange): Parsed<List<FinalVendor>> {
        val issues = check(range, Tabs.VENDOR, CHECKS)
        if (issues.isNotEmpty()) return Parsed(null, issues)
        return Parsed(
            ((HEADER_ROW + 1)..LAST_ROW).mapNotNull { r ->
                val row = range.row(r)
                val category = row['B'].textOrNull() ?: return@mapNotNull null
                FinalVendor(
                    category = category,
                    sortOrder = r,
                    vendorName = row['D'].textOrNull(),
                    paymentRaw = row['F'].textOrNull(),
                    meetingRaw = row['G'].textOrNull(),
                    notes = row['H'].textOrNull(),
                )
            },
        )
    }
}

/** `_APP_CONFIG!A1:C…`: `email | side | role` (§5.9). Rows with an unknown role are skipped. */
object AppConfigParser {
    fun parse(range: ValueRange): List<AppUser> = range.rows.drop(1).mapNotNull { row ->
        val email = row['A'].textOrNull()?.lowercase() ?: return@mapNotNull null
        val role = Role.entries.firstOrNull { it.name.equals(row['C'].textOrNull(), ignoreCase = true) } ?: return@mapNotNull null
        val side = Side.entries.firstOrNull { it.name.equals(row['B'].textOrNull(), ignoreCase = true) }
        AppUser(email, side, role)
    }
}
