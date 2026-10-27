package app.weddingplanner.core.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface WeddingDao {
    @Query("SELECT * FROM budget_item ORDER BY budget_table, sheet_row")
    fun budgetItems(): Flow<List<BudgetItemEntity>>

    @Query("SELECT * FROM budget_item WHERE row_id = :rowId")
    fun budgetItem(rowId: String): Flow<BudgetItemEntity?>

    @Query("SELECT * FROM budget_summary WHERE id = 1")
    fun budgetSummary(): Flow<BudgetSummaryEntity?>

    @Query("SELECT * FROM final_vendor ORDER BY sheet_row")
    fun finalVendors(): Flow<List<FinalVendorEntity>>

    @Query("SELECT * FROM wedding_event")
    fun weddingEvents(): Flow<List<WeddingEventEntity>>

    @Query("SELECT * FROM app_user")
    suspend fun appUsers(): List<AppUserEntity>

    @Query("SELECT * FROM row_identity WHERE table_key = :tableKey")
    suspend fun rowIdentities(tableKey: String): List<RowIdentityEntity>

    @Query("SELECT * FROM sync_run WHERE result NOT IN ('ERROR', 'OFFLINE', 'AUTH_REQUIRED') ORDER BY finished_at DESC LIMIT 1")
    fun lastSuccessfulRun(): Flow<SyncRunEntity?>

    @Query("SELECT payload_hash FROM sync_run WHERE payload_hash IS NOT NULL ORDER BY finished_at DESC LIMIT 1")
    suspend fun lastPayloadHash(): String?

    @Transaction
    suspend fun replaceBudget(items: List<BudgetItemEntity>, summary: BudgetSummaryEntity, identities: Map<String, List<RowIdentityEntity>>) {
        clearBudgetItems()
        insertBudgetItems(items)
        insertSummary(summary)
        identities.forEach { (tableKey, rows) ->
            clearRowIdentities(tableKey)
            insertRowIdentities(rows)
        }
    }

    @Transaction
    suspend fun replaceVendors(rows: List<FinalVendorEntity>) {
        clearVendors()
        insertVendors(rows)
    }

    @Transaction
    suspend fun replaceEvents(rows: List<WeddingEventEntity>) {
        clearEvents()
        insertEvents(rows)
    }

    @Transaction
    suspend fun replaceUsers(rows: List<AppUserEntity>) {
        clearUsers()
        insertUsers(rows)
    }

    @Insert
    suspend fun insertSyncRun(run: SyncRunEntity)

    @Query("DELETE FROM sync_run WHERE id NOT IN (SELECT id FROM sync_run ORDER BY finished_at DESC LIMIT 200)")
    suspend fun trimSyncRuns()

    @Query("DELETE FROM budget_item") suspend fun clearBudgetItems()
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertBudgetItems(rows: List<BudgetItemEntity>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertSummary(row: BudgetSummaryEntity)
    @Query("DELETE FROM row_identity WHERE table_key = :tableKey") suspend fun clearRowIdentities(tableKey: String)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertRowIdentities(rows: List<RowIdentityEntity>)
    @Query("DELETE FROM final_vendor") suspend fun clearVendors()
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertVendors(rows: List<FinalVendorEntity>)
    @Query("DELETE FROM wedding_event") suspend fun clearEvents()
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertEvents(rows: List<WeddingEventEntity>)
    @Query("DELETE FROM app_user") suspend fun clearUsers()
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertUsers(rows: List<AppUserEntity>)
}
