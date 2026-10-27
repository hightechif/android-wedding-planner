package app.weddingplanner.core.database

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

// Cache of the sheet (docs/05-DATA-SCHEMA.md §3). The sheet is authoritative; every table here
// can be rebuilt by a pull. `*_raw` columns keep the exact sheet string.

@Entity(
    tableName = "budget_item",
    indices = [Index("budget_table", "due_epoch_day"), Index("status_raw")],
)
data class BudgetItemEntity(
    @PrimaryKey val row_id: String,
    val budget_table: String,
    val sheet_row: Int,
    val no: Int?,
    val category_raw: String,
    val detail: String,
    val vendor_or_payer: String?,
    val qty: Double?,
    val unit: String?,
    val price: Long?,
    val price_formula: String?,
    val total: Long,
    val paid: Long,
    val remaining: Long?,
    val progress: Double?,
    val status_raw: String?,
    val due_epoch_day: Long?,
    val notes: String?,
)

@Entity(tableName = "budget_summary")
data class BudgetSummaryEntity(
    @PrimaryKey val id: Int = 1,
    val budgeting_nikah: Long,
    val budget_terpakai: Double,
    val total_estimasi: Long,
    val selisih: Long,
    val total_akad_resepsi: Long,
    val total_mahar_seserahan: Long,
    val total_biaya: Long,
    val sudah_dibayar: Long,
    val sisa: Long,
    val progress: Double,
    val item_lunas: Int?,
    val item_total: Int?,
    val due_14_days: Int?,
)

@Entity(tableName = "final_vendor")
data class FinalVendorEntity(
    @PrimaryKey val category: String,
    val sheet_row: Int,
    val vendor_name: String?,
    val payment_raw: String?,
    val meeting_raw: String?,
    val notes: String?,
)

@Entity(tableName = "wedding_event")
data class WeddingEventEntity(
    @PrimaryKey val type: String,
    val epoch_day: Long,
)

@Entity(tableName = "app_user")
data class AppUserEntity(
    @PrimaryKey val email: String,
    val side: String?,
    val role: String,
)

/** Last known fingerprint per row ID, for sort repair (TRD §5.3). */
@Entity(tableName = "row_identity", indices = [Index("table_key", "sheet_row")])
data class RowIdentityEntity(
    @PrimaryKey val row_id: String,
    val table_key: String,
    val sheet_row: Int,
    val fingerprint: String,
)

@Entity(tableName = "sync_run")
data class SyncRunEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val started_at: Long,
    val finished_at: Long,
    val trigger: String,
    /** A [app.weddingplanner.core.model.SyncState] name, or "UNCHANGED". */
    val result: String,
    val payload_hash: String?,
    val rows_pulled: Int,
    val ids_written: Int,
    /** Layout mismatches and errors, newline-separated. No cell values (no PII). */
    val error: String?,
)
