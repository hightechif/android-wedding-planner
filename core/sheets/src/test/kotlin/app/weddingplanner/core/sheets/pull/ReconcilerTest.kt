package app.weddingplanner.core.sheets.pull

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class ReconcilerTest {
    private val a = "BUD-AAAAAAAA"
    private val b = "BUD-BBBBBBBB"
    private val c = "BUD-CCCCCCCC"
    private val known = listOf(KnownRow(a, "fa"), KnownRow(b, "fb"), KnownRow(c, "fc"))

    private fun row(sheetRow: Int, id: String?, fp: String) = ObservedRow(sheetRow, id, fp, Unit)
    private fun run(vararg rows: ObservedRow<Unit>, knownRows: List<KnownRow> = known) =
        Reconciler.reconcile("BUD", rows.toList(), knownRows, Random(1))

    @Test
    fun `unchanged rows need no writes`() {
        val r = run(row(26, a, "fa"), row(27, b, "fb"), row(28, c, "fc"))
        assertTrue(r.idWrites.isEmpty())
        assertTrue(r.events.isEmpty())
    }

    @Test
    fun `a sort without column A is repaired by fingerprint`() {
        // Values sorted C, A, B while column A stayed A, B, C.
        val r = run(row(26, a, "fc"), row(27, b, "fa"), row(28, c, "fb"))
        assertEquals(mapOf(26 to c, 27 to a, 28 to b), r.ids)
        assertEquals(mapOf(26 to c, 27 to a, 28 to b), r.idWrites)
        assertEquals(3, r.events[ReconcileEvent.SORT_REPAIRED])
    }

    @Test
    fun `an edited row keeps its id`() {
        val r = run(row(26, a, "fa-edited"), row(27, b, "fb"), row(28, c, "fc"))
        assertEquals(a, r.ids[26])
        assertTrue(r.idWrites.isEmpty())
    }

    @Test
    fun `new rows, duplicates and deletions`() {
        val r = run(row(26, a, "fa"), row(27, a, "fa-copy"), row(28, null, "fnew"))
        assertEquals(a, r.ids[26])
        assertEquals(setOf(27, 28), r.idWrites.keys)
        assertTrue(r.idWrites.values.none { it == a || it == b || it == c })
        assertEquals(setOf(b, c), r.deleted)
        assertEquals(1, r.events[ReconcileEvent.DUPLICATE_ID])
        assertEquals(1, r.events[ReconcileEvent.NEW_ROW])
    }

    @Test
    fun `first sync on a device trusts column A`() {
        val r = run(row(26, a, "fa"), row(27, b, "anything"), knownRows = emptyList())
        assertEquals(mapOf(26 to a, 27 to b), r.ids)
        assertTrue(r.idWrites.isEmpty())
    }
}
