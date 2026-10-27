package app.weddingplanner.core.data

import app.weddingplanner.core.database.WeddingDao
import app.weddingplanner.core.database.WeddingDatabase
import app.weddingplanner.core.model.BudgetItem
import app.weddingplanner.core.model.BudgetSummary
import app.weddingplanner.core.model.FinalVendor
import app.weddingplanner.core.model.WeddingEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/** Read side of the local cache. Screens observe these flows; [SyncEngine] fills them. */
@Singleton
class WeddingRepository @Inject constructor(
    private val db: WeddingDatabase,
    private val dao: WeddingDao,
) {
    val budgetItems: Flow<List<BudgetItem>> = dao.budgetItems().map { rows -> rows.map { it.toModel() } }
    val budgetSummary: Flow<BudgetSummary?> = dao.budgetSummary().map { it?.toModel() }
    val finalVendors: Flow<List<FinalVendor>> = dao.finalVendors().map { rows -> rows.map { it.toModel() } }
    val events: Flow<List<WeddingEvent>> = dao.weddingEvents().map { rows -> rows.mapNotNull { it.toModel() } }

    fun budgetItem(rowId: String): Flow<BudgetItem?> = dao.budgetItem(rowId).map { it?.toModel() }

    /** Sign-out and re-linking drop the cache (PRD ONB-1). */
    suspend fun clearLocalData() = withContext(Dispatchers.IO) { db.clearAllTables() }
}
