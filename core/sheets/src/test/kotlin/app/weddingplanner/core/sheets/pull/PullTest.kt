package app.weddingplanner.core.sheets.pull

import app.weddingplanner.core.model.BudgetTable
import app.weddingplanner.core.model.EventType
import app.weddingplanner.core.sheets.CellValue
import app.weddingplanner.core.sheets.TemplateFixture
import app.weddingplanner.core.sheets.layout.Tabs
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class PullTest {
    @Test
    fun `parses budget, summary, events and vendors from the template`() = runTest {
        val sheet = TemplateFixture.create()
        sheet.tab(Tabs.BUDGET).apply {
            set(26, 'J', CellValue.Number(50_000.0))
            text(26, 'M', "DP")
            set(26, 'N', CellValue.Number(46400.0))
            set(81, 'H', CellValue.Formula("=5000000/F81"))
        }
        val snap = SheetPuller(sheet).pull("id", hasAppConfig = false)

        assertTrue(snap.issues.toString(), snap.issues.isEmpty())
        val budget = snap.budget.value!!
        assertEquals(listOf("Dokumen", "Catering"), budget.akad.map { it.value.detail })
        assertEquals(BudgetTable.MAHAR_SESERAHAN, budget.mahar.single().value.table)
        assertEquals("=5000000/F81", budget.mahar.single().value.priceFormula)
        val first = budget.akad.first().value
        assertEquals(50_000L, first.paid)
        assertEquals(LocalDate.of(2027, 1, 13), first.dueDate)
        assertEquals("administrasi|dokumen|", budget.akad.first().fingerprint)
        assertEquals(129_869_604L, budget.summary.totalBiaya)
        assertEquals(10, budget.summary.itemLunas)
        assertEquals(38, budget.summary.itemTotal)
        assertEquals(0, budget.summary.dueIn14Days)
        assertEquals(LocalDate.of(2027, 1, 17), snap.events.value!!.single { it.type == EventType.WEDDING }.date)
        assertEquals(listOf("Jasa WO", "Dekorasi"), snap.vendors.value!!.map { it.category })
        assertNull(snap.users)
    }

    @Test
    fun `rows inserted above the Mahar table shift it via the sentinel`() = runTest {
        val sheet = TemplateFixture.create()
        val budget = sheet.tab(Tabs.BUDGET)
        // Simulate two rows inserted at row 76: everything from row 76 down moves by 2.
        val moved = budget.cells.filterKeys { it.first >= 76 }
        moved.keys.forEach { budget.cells.remove(it) }
        moved.forEach { (k, v) -> budget.cells[(k.first + 2) to k.second] = v }

        val parsed = SheetPuller(sheet).pull("id", hasAppConfig = false).budget
        assertTrue(parsed.issues.toString(), parsed.isValid)
        assertEquals(2, parsed.value!!.offset)
        assertEquals(83, parsed.value!!.mahar.single().sheetRow)
    }

    @Test
    fun `a renamed header makes only that module invalid`() = runTest {
        val sheet = TemplateFixture.create()
        sheet.tab(Tabs.BUDGET).text(24, 'E', "Vendor Name")
        val snap = SheetPuller(sheet).pull("id", hasAppConfig = false)
        assertNull(snap.budget.value)
        assertTrue(snap.budget.issues.single().contains("E24"))
        assertTrue(snap.vendors.isValid && snap.events.isValid)
    }

    @Test
    fun `payload hash changes only when the sheet does`() = runTest {
        val sheet = TemplateFixture.create()
        val puller = SheetPuller(sheet)
        val a = puller.pull("id", false).payloadHash
        assertEquals(a, puller.pull("id", false).payloadHash)
        sheet.tab(Tabs.BUDGET).set(26, 'J', CellValue.Number(1.0))
        assertTrue(a != puller.pull("id", false).payloadHash)
    }
}
