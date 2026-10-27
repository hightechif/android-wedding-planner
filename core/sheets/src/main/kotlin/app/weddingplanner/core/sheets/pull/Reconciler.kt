package app.weddingplanner.core.sheets.pull

import app.weddingplanner.core.sheets.RowIds
import kotlin.random.Random

/** A row identity this device has seen before (`row_identity`). */
data class KnownRow(val id: String, val fingerprint: String)

enum class ReconcileEvent { SORT_REPAIRED, NEW_ROW, DUPLICATE_ID, DELETED_REMOTELY }

data class ReconcileResult(
    /** sheetRow → final row ID, for every observed row. */
    val ids: Map<Int, String>,
    /** sheetRow → ID to write into column A (new rows, duplicates, sort repairs). */
    val idWrites: Map<Int, String>,
    /** Known IDs no longer in the sheet. */
    val deleted: Set<String>,
    val events: Map<ReconcileEvent, Int>,
)

/**
 * Row identity after every pull (TRD §5.3).
 *
 * A row whose `_id` points at a known row with a *different* fingerprint, while another known row
 * has exactly this row's fingerprint, was moved by a sort that left column A behind: it takes that
 * other ID back. A fingerprint change with no such match is an ordinary edit and keeps its ID.
 * Rows without an ID are new; repeated IDs (copy-pasted rows) keep the first and re-issue the rest.
 */
object Reconciler {
    fun <T> reconcile(
        idPrefix: String,
        observed: List<ObservedRow<T>>,
        known: List<KnownRow>,
        random: Random = Random.Default,
    ): ReconcileResult {
        val knownFp = known.associate { it.id to it.fingerprint }
        val uniqueByFp = known.groupBy { it.fingerprint }.filterValues { it.size == 1 }.mapValues { it.value.single().id }
        val events = mutableMapOf<ReconcileEvent, Int>()
        fun count(e: ReconcileEvent) { events[e] = (events[e] ?: 0) + 1 }

        // Column A as read, with repeated IDs dropped after their first occurrence.
        val seen = mutableSetOf<String>()
        val cellId = observed.associate { row ->
            val id = row.id?.takeIf { RowIds.isValid(it) }
            row.sheetRow to when {
                id == null -> null
                seen.add(id) -> id
                else -> null.also { count(ReconcileEvent.DUPLICATE_ID) }
            }
        }

        val claimed = mutableSetOf<String>()
        val ids = mutableMapOf<Int, String>()

        // 1. In place: the ID is unknown to this device, or its fingerprint still matches.
        observed.forEach { row ->
            val id = cellId[row.sheetRow] ?: return@forEach
            val fp = knownFp[id]
            if (fp == null || fp == row.fingerprint) {
                ids[row.sheetRow] = id
                claimed += id
            }
        }
        // 2. Moved: take back the ID whose fingerprint this row carries.
        observed.forEach { row ->
            if (row.sheetRow in ids || cellId[row.sheetRow] == null) return@forEach
            val owner = uniqueByFp[row.fingerprint]
            if (owner != null && owner !in claimed) {
                ids[row.sheetRow] = owner
                claimed += owner
                count(ReconcileEvent.SORT_REPAIRED)
            }
        }
        // 3. Edited in place: keep the cell's ID if nobody took it in step 2.
        observed.forEach { row ->
            val id = cellId[row.sheetRow]
            if (row.sheetRow !in ids && id != null && id !in claimed) {
                ids[row.sheetRow] = id
                claimed += id
            }
        }
        // 4. New, duplicate, or displaced rows get a fresh ID.
        val taken = (known.map { it.id } + observed.mapNotNull { it.id }).toMutableSet()
        observed.forEach { row ->
            if (row.sheetRow in ids) return@forEach
            val id = RowIds.generate(idPrefix, taken, random)
            taken += id
            ids[row.sheetRow] = id
            if (row.id == null) count(ReconcileEvent.NEW_ROW)
        }

        val deleted = knownFp.keys - ids.values.toSet()
        if (deleted.isNotEmpty()) events[ReconcileEvent.DELETED_REMOTELY] = deleted.size
        return ReconcileResult(
            ids = ids,
            idWrites = observed.filter { ids[it.sheetRow] != it.id }.associate { it.sheetRow to ids.getValue(it.sheetRow) },
            deleted = deleted,
            events = events,
        )
    }
}
