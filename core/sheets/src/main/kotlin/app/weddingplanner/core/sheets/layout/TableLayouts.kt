package app.weddingplanner.core.sheets.layout

import app.weddingplanner.core.sheets.CellValue
import app.weddingplanner.core.sheets.SheetRow

/** A fixed cell whose text must match for the layout to be trusted (header or sentinel). */
data class CellCheck(val row: Int, val column: Char, val expected: String)

/**
 * A table whose rows get a stable `_id` in hidden column A (TRD §5.3).
 * Ranges are 1-based A1 rows and verified against the template (docs/05-DATA-SCHEMA.md §5).
 */
data class TableLayout(
    val key: String,
    val tab: String,
    val idPrefix: String,
    val headerRow: Int,
    val firstDataRow: Int,
    /** Inclusive; null = until the end of the tab. */
    val lastDataRow: Int?,
    val lastColumn: Char,
    val checks: List<CellCheck>,
    val isDataRow: (SheetRow) -> Boolean,
) {
    /** First and last row to read so that checks, header and data are all covered. */
    val readFirstRow: Int get() = minOf(headerRow, checks.minOfOrNull { it.row } ?: headerRow)
    val readLastRow: Int?
        get() = lastDataRow?.let { last -> maxOf(last, checks.maxOfOrNull { it.row } ?: last) }

    override fun toString() = "TableLayout($key)"
}

object Tabs {
    const val DASHBOARD = "DASHBOARD"
    const val PRA_NIKAH = "Checklist Pra Nikah"
    const val PERNIKAHAN = "Checklist Pernikahan"
    const val VENDOR = "Perbandingan Vendor"
    const val MEDICAL = "Checklist Medical Check up & Perawatan"
    const val GUEST_CPW = "Undangan CPW"
    const val GUEST_CPP = "Undangan CPP"
    const val BUDGET = "WEDDING BUDGET"
    const val APP_CONFIG = "_APP_CONFIG"

    /** Every tab the app reads; linking fails if one is missing. */
    val REQUIRED = listOf(DASHBOARD, PRA_NIKAH, PERNIKAHAN, VENDOR, MEDICAL, GUEST_CPW, GUEST_CPP, BUDGET)
}

private fun SheetRow.hasText(column: Char) = !this[column].isBlank
private fun SheetRow.isBool(column: Char) = this[column] is CellValue.Bool

private val checklistDataRow: (SheetRow) -> Boolean = { it.isBool('H') || it.isBool('I') }

private fun checklistChecks(headerRow: Int) = listOf(
    CellCheck(headerRow, 'B', "NO"),
    CellCheck(headerRow, 'C', "ITEM"),
    CellCheck(headerRow, 'H', "STATUS"),
    CellCheck(headerRow, 'J', "DETAIL / KETERANGAN"),
    CellCheck(headerRow, 'K', "PIC"),
)

private fun guestTable(key: String, tab: String) = TableLayout(
    key = key,
    tab = tab,
    idPrefix = "GST",
    headerRow = 2,
    firstDataRow = 3,
    lastDataRow = 178,
    lastColumn = 'H',
    checks = listOf(
        CellCheck(2, 'B', "No."),
        CellCheck(2, 'C', "Kategori"),
        CellCheck(2, 'E', "Nama"),
        CellCheck(2, 'H', "Hadir"),
        CellCheck(180, 'B', "TOTAL COUNT \"YA\""),
    ),
    isDataRow = { it.hasText('E') },
)

object TableLayouts {
    val BUDGET_AKAD = TableLayout(
        key = "budget_akad",
        tab = Tabs.BUDGET,
        idPrefix = "BUD",
        headerRow = 24,
        firstDataRow = 26,
        lastDataRow = 75,
        lastColumn = 'O',
        checks = listOf(
            CellCheck(24, 'B', "No"),
            CellCheck(24, 'C', "Category"),
            CellCheck(24, 'D', "Detail Item"),
            CellCheck(24, 'E', "Vendor"),
            CellCheck(24, 'J', "Sudah Dibayar"),
            CellCheck(24, 'N', "Jatuh Tempo"),
            CellCheck(77, 'B', "MAHAR & SESERAHAN"),
        ),
        isDataRow = { it.hasText('D') },
    )

    val BUDGET_MAHAR = TableLayout(
        key = "budget_mahar",
        tab = Tabs.BUDGET,
        idPrefix = "BUD",
        headerRow = 79,
        firstDataRow = 81,
        lastDataRow = 106,
        lastColumn = 'O',
        checks = listOf(
            CellCheck(79, 'B', "No"),
            CellCheck(79, 'C', "Category"),
            CellCheck(79, 'D', "Detailed Item"),
            CellCheck(79, 'E', "Groom / Bride"),
            CellCheck(79, 'J', "Sudah Dibayar"),
            CellCheck(79, 'N', "Jatuh Tempo"),
        ),
        isDataRow = { it.hasText('D') },
    )

    val CHECKLIST_PRA_NIKAH = TableLayout(
        key = "checklist_pra_nikah",
        tab = Tabs.PRA_NIKAH,
        idPrefix = "CHK",
        headerRow = 16,
        firstDataRow = 18,
        lastDataRow = null,
        lastColumn = 'K',
        checks = checklistChecks(16),
        isDataRow = checklistDataRow,
    )

    val CHECKLIST_PERNIKAHAN = TableLayout(
        key = "checklist_pernikahan",
        tab = Tabs.PERNIKAHAN,
        idPrefix = "CHK",
        headerRow = 15,
        firstDataRow = 17,
        lastDataRow = null,
        lastColumn = 'K',
        checks = checklistChecks(15),
        isDataRow = checklistDataRow,
    )

    val MEDICAL = TableLayout(
        key = "medical",
        tab = Tabs.MEDICAL,
        idPrefix = "MED",
        headerRow = 4,
        firstDataRow = 5,
        lastDataRow = 11,
        lastColumn = 'E',
        checks = listOf(CellCheck(4, 'B', "Nama Pemeriksaan"), CellCheck(4, 'E', "Check")),
        isDataRow = { it.hasText('B') && it.isBool('E') },
    )

    val PERAWATAN = TableLayout(
        key = "perawatan",
        tab = Tabs.MEDICAL,
        idPrefix = "PRW",
        headerRow = 22,
        firstDataRow = 23,
        lastDataRow = null,
        lastColumn = 'E',
        checks = listOf(CellCheck(22, 'B', "Waktu"), CellCheck(22, 'C', "Kegiatan"), CellCheck(22, 'E', "Check")),
        isDataRow = { it.hasText('C') && it.isBool('E') },
    )

    val GUEST_CPP = guestTable("guest_cpp", Tabs.GUEST_CPP)
    val GUEST_CPW = guestTable("guest_cpw", Tabs.GUEST_CPW)

    /** Tables that receive an `_id` column in the M0 migration. */
    val ID_TABLES = listOf(
        BUDGET_AKAD, BUDGET_MAHAR,
        CHECKLIST_PRA_NIKAH, CHECKLIST_PERNIKAHAN,
        MEDICAL, PERAWATAN,
        GUEST_CPP, GUEST_CPW,
    )
}
