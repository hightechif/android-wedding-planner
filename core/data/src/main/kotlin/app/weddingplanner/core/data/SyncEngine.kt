package app.weddingplanner.core.data

import app.weddingplanner.core.auth.AuthRequiredException
import app.weddingplanner.core.database.RowIdentityEntity
import app.weddingplanner.core.database.SyncRunEntity
import app.weddingplanner.core.database.WeddingDao
import app.weddingplanner.core.datastore.UserPreferencesRepository
import app.weddingplanner.core.model.AppUser
import app.weddingplanner.core.model.BudgetItem
import app.weddingplanner.core.model.Role
import app.weddingplanner.core.model.SyncState
import app.weddingplanner.core.model.SyncStatus
import app.weddingplanner.core.sheets.SheetsException
import app.weddingplanner.core.sheets.SheetsGateway
import app.weddingplanner.core.sheets.SpreadsheetMeta
import app.weddingplanner.core.sheets.layout.CURRENT_SCHEMA_VERSION
import app.weddingplanner.core.sheets.layout.SCHEMA_VERSION_KEY
import app.weddingplanner.core.sheets.layout.TableLayouts
import app.weddingplanner.core.sheets.layout.Tabs
import app.weddingplanner.core.sheets.pull.BudgetPull
import app.weddingplanner.core.sheets.pull.KnownRow
import app.weddingplanner.core.sheets.pull.ObservedRow
import app.weddingplanner.core.sheets.pull.ReconcileEvent
import app.weddingplanner.core.sheets.pull.Reconciler
import app.weddingplanner.core.sheets.pull.SheetPuller
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.IOException
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

/** [MANUAL], [LINKED] and [MIGRATED] re-read the sheet's tabs and schema version instead of using the hourly cache. */
enum class SyncTrigger { FOREGROUND, MANUAL, LINKED, MIGRATED }

/**
 * Read-only sync (M1): pull → validate → reconcile row IDs → replace the cache (TRD §5.4, without
 * merge/push). The only write is column A of `WEDDING BUDGET`, when the Reconciler assigns an ID
 * to a row added in Sheets or repairs a sort. Failures never clear the cache.
 */
@Singleton
class SyncEngine @Inject constructor(
    private val gateway: SheetsGateway,
    private val dao: WeddingDao,
    private val preferences: UserPreferencesRepository,
) {
    private val puller = SheetPuller(gateway)
    private val mutex = Mutex()
    private val live = MutableStateFlow(SyncStatus())
    private var meta: SpreadsheetMeta? = null
    private var metaCheckedAt = 0L

    val status: Flow<SyncStatus> = combine(live, dao.lastSuccessfulRun()) { s, run ->
        s.copy(lastSuccessAt = run?.finished_at?.let(Instant::ofEpochMilli))
    }

    /** Pulls now unless a sync is already running (then waits for it and returns). */
    suspend fun sync(trigger: SyncTrigger) {
        if (mutex.isLocked && trigger == SyncTrigger.FOREGROUND) return
        mutex.withLock { runSync(trigger) }
    }

    /**
     * Foreground polling (TRD §5.7): call from a lifecycle-scoped coroutine while the app is visible.
     * Starts at once when a sheet gets linked, then pulls every 60 s.
     */
    suspend fun pollWhileVisible() {
        preferences.preferences
            .map { if (it.isLinked) it.linkedSpreadsheetId else null }
            .distinctUntilChanged()
            .collectLatest { id ->
                if (id == null) return@collectLatest
                sync(SyncTrigger.LINKED)
                while (true) {
                    delay(POLL_INTERVAL_MILLIS)
                    sync(SyncTrigger.FOREGROUND)
                }
            }
    }

    private suspend fun runSync(trigger: SyncTrigger) {
        val prefs = preferences.current()
        val spreadsheetId = prefs.linkedSpreadsheetId ?: return
        val startedAt = System.currentTimeMillis()
        live.value = live.value.copy(state = SyncState.SYNCING)
        if (trigger != SyncTrigger.FOREGROUND) metaCheckedAt = 0

        val outcome = try {
            pullAndStore(spreadsheetId, prefs.accountEmail)
        } catch (e: AuthRequiredException) {
            Outcome(SyncState.AUTH_REQUIRED, message = e.message)
        } catch (e: IOException) {
            Outcome(SyncState.OFFLINE, message = "Offline. Data terakhir tetap ditampilkan.")
        } catch (e: SheetsException) {
            Outcome(SyncState.ERROR, message = e.googleMessage ?: "Gagal sinkronisasi (${e.httpCode}).")
        }

        live.value = SyncStatus(state = outcome.state, message = outcome.message, mismatches = outcome.mismatches)
        dao.insertSyncRun(
            SyncRunEntity(
                started_at = startedAt,
                finished_at = System.currentTimeMillis(),
                trigger = trigger.name,
                result = if (outcome.unchanged) "UNCHANGED" else outcome.state.name,
                payload_hash = outcome.payloadHash,
                rows_pulled = outcome.rowsPulled,
                ids_written = outcome.idsWritten,
                error = (outcome.mismatches + listOfNotNull(outcome.message)).joinToString("\n").ifEmpty { null },
            ),
        )
        dao.trimSyncRuns()
    }

    private suspend fun pullAndStore(spreadsheetId: String, email: String?): Outcome {
        val meta = metaFor(spreadsheetId)
        val version = meta.developerMetadata[SCHEMA_VERSION_KEY]?.toIntOrNull() ?: 0
        if (version < CURRENT_SCHEMA_VERSION) {
            return Outcome(SyncState.SCHEMA_MISMATCH, message = "Sheet perlu dimigrasi (Penyiapan Sheet).")
        }

        val snapshot = puller.pull(spreadsheetId, hasAppConfig = meta.sheet(Tabs.APP_CONFIG) != null)
        if (snapshot.payloadHash == dao.lastPayloadHash() && snapshot.issues.isEmpty()) {
            return Outcome(SyncState.IDLE, payloadHash = snapshot.payloadHash, unchanged = true)
        }

        snapshot.users?.let { users -> dao.replaceUsers(users.map { it.toEntity() }) }
        val users = snapshot.users ?: dao.appUsers().map { it.toModel() }

        var idsWritten = 0
        var rows = 0
        var message: String? = null
        snapshot.budget.value?.let { budget ->
            val result = storeBudget(spreadsheetId, budget, canWriteIds(users, email))
            idsWritten = result.first
            rows = budget.akad.size + budget.mahar.size
            message = result.second
        }
        snapshot.events.value?.let { events -> dao.replaceEvents(events.map { it.toEntity() }) }
        snapshot.vendors.value?.let { vendors -> dao.replaceVendors(vendors.map { it.toEntity() }) }

        val issues = snapshot.issues
        return Outcome(
            state = if (issues.isEmpty()) SyncState.IDLE else SyncState.SCHEMA_MISMATCH,
            message = message,
            mismatches = issues,
            payloadHash = snapshot.payloadHash,
            rowsPulled = rows,
            idsWritten = idsWritten,
        )
    }

    /** Returns (IDs written, a note for the user or null). */
    private suspend fun storeBudget(spreadsheetId: String, budget: BudgetPull, canWrite: Boolean): Pair<Int, String?> {
        val tables = listOf(TableLayouts.BUDGET_AKAD to budget.akad, TableLayouts.BUDGET_MAHAR to budget.mahar)
        val results = tables.map { (layout, observed) ->
            val known = dao.rowIdentities(layout.key).map { KnownRow(it.row_id, it.fingerprint) }
            Triple(layout.key, observed, Reconciler.reconcile(layout.idPrefix, observed, known))
        }
        val writes = results.flatMap { it.third.idWrites.entries }.associate { it.key to it.value }
        var written = 0
        var note: String? = null
        if (writes.isNotEmpty() && canWrite) {
            try {
                puller.writeIds(spreadsheetId, writes)
                written = writes.size
                if (results.any { (it.third.events[ReconcileEvent.SORT_REPAIRED] ?: 0) > 0 }) {
                    note = "Urutan baris di sheet berubah; ID baris sudah diperbaiki."
                }
            } catch (e: SheetsException) {
                // Read-only share or a protected range: keep the local IDs and try again next pull.
                note = "ID baris baru belum bisa ditulis ke sheet (${e.httpCode})."
            }
        }

        val items = mutableListOf<BudgetItem>()
        val identities = results.associate { (key, observed, result) ->
            key to observed.map { row: ObservedRow<BudgetItem> ->
                val id = result.ids.getValue(row.sheetRow)
                items += row.value.copy(rowId = id)
                RowIdentityEntity(row_id = id, table_key = key, sheet_row = row.sheetRow, fingerprint = row.fingerprint)
            }
        }
        dao.replaceBudget(items.map { it.toEntity() }, budget.summary.toEntity(), identities)
        return written to note
    }

    /** The WO (VIEWER) never writes. Before `_APP_CONFIG` lists anyone, the signed-in owner may. */
    private fun canWriteIds(users: List<AppUser>, email: String?): Boolean {
        if (users.isEmpty()) return true
        val role = users.firstOrNull { it.email.equals(email, ignoreCase = true) }?.role ?: return false
        return role != Role.VIEWER
    }

    private suspend fun metaFor(spreadsheetId: String): SpreadsheetMeta {
        val cached = meta
        val now = System.currentTimeMillis()
        if (cached != null && cached.spreadsheetId == spreadsheetId && now - metaCheckedAt < META_TTL_MILLIS) return cached
        return gateway.getSpreadsheet(spreadsheetId).also {
            meta = it
            metaCheckedAt = now
        }
    }

    private data class Outcome(
        val state: SyncState,
        val message: String? = null,
        val mismatches: List<String> = emptyList(),
        val payloadHash: String? = null,
        val rowsPulled: Int = 0,
        val idsWritten: Int = 0,
        val unchanged: Boolean = false,
    )

    private companion object {
        const val POLL_INTERVAL_MILLIS = 60_000L
        const val META_TTL_MILLIS = 60 * 60 * 1000L
    }
}
