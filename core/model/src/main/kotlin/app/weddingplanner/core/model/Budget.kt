package app.weddingplanner.core.model

import java.time.Instant
import java.time.LocalDate

typealias RowId = String

/** Whole rupiah, no decimals. */
typealias Rupiah = Long

enum class BudgetTable { AKAD_RESEPSI, MAHAR_SESERAHAN }

/** Budget `M` (a formula). Unknown strings are kept raw in [BudgetItem.statusRaw]. */
enum class PaymentStatus(val sheetValue: String) {
    LUNAS("Lunas"), DP("DP"), BELUM_BAYAR("Belum bayar");

    companion object {
        fun fromSheet(raw: String?): PaymentStatus? = entries.firstOrNull { it.sheetValue.equals(raw?.trim(), ignoreCase = true) }
    }
}

/** One row of Akad & Resepsi or Mahar & Seserahan (docs/05-DATA-SCHEMA.md §5.1–5.2). */
data class BudgetItem(
    val rowId: RowId,
    val table: BudgetTable,
    val sheetRow: Int,
    val no: Int?,
    val categoryRaw: String,
    val detail: String,
    /** Akad: Vendor (`E`). Mahar: Groom / Bride (`E`). */
    val vendorOrPayer: String?,
    val qty: Double?,
    val unit: String?,
    val price: Rupiah?,
    val priceFormula: String?,
    val total: Rupiah,
    val paid: Rupiah,
    val remaining: Rupiah?,
    val progress: Double?,
    val statusRaw: String?,
    val dueDate: LocalDate?,
    val notes: String?,
) {
    val status: PaymentStatus? get() = PaymentStatus.fromSheet(statusRaw)
    val isPriceMissing: Boolean get() = price == null || price == 0L
}

/** `WEDDING BUDGET` summary boxes (§5.3). Values are the sheet's, never recomputed. */
data class BudgetSummary(
    val budgetingNikah: Rupiah,
    val budgetTerpakai: Double,
    val totalEstimasi: Rupiah,
    val selisih: Rupiah,
    val totalAkadResepsi: Rupiah,
    val totalMaharSeserahan: Rupiah,
    val totalBiaya: Rupiah,
    val sudahDibayar: Rupiah,
    val sisaPembayaran: Rupiah,
    val progress: Double,
    val itemLunas: Int?,
    val itemTotal: Int?,
    val dueIn14Days: Int?,
)

/** A Finalisasi Vendor row (§5.7). [category] is the natural key. */
data class FinalVendor(
    val category: String,
    val sortOrder: Int,
    val vendorName: String?,
    val paymentRaw: String?,
    val meetingRaw: String?,
    val notes: String?,
)

data class WeddingEvent(val type: EventType, val date: LocalDate)

/** A row of `_APP_CONFIG`. [side] is null for a VIEWER. */
data class AppUser(val email: String, val side: Side?, val role: Role)

enum class SyncState { IDLE, SYNCING, OFFLINE, ERROR, SCHEMA_MISMATCH, AUTH_REQUIRED }

data class SyncStatus(
    val state: SyncState = SyncState.IDLE,
    val lastSuccessAt: Instant? = null,
    val message: String? = null,
    /** One line per table whose layout didn't match, e.g. "WEDDING BUDGET!E24: …". */
    val mismatches: List<String> = emptyList(),
)
