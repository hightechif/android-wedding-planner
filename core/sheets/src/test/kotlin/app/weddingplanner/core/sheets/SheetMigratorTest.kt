package app.weddingplanner.core.sheets

import app.weddingplanner.core.model.AccountSeed
import app.weddingplanner.core.model.Role
import app.weddingplanner.core.model.Side
import app.weddingplanner.core.sheets.layout.CURRENT_SCHEMA_VERSION
import app.weddingplanner.core.sheets.layout.SCHEMA_VERSION_KEY
import app.weddingplanner.core.sheets.layout.Tabs
import app.weddingplanner.core.sheets.migration.ID_HEADER
import app.weddingplanner.core.sheets.migration.ID_PROTECTION_DESCRIPTION
import app.weddingplanner.core.sheets.migration.SheetMigrator
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class SheetMigratorTest {
    private val accounts = listOf(
        AccountSeed("Owner@Example.com", Side.CPP, Role.OWNER),
        AccountSeed("partner@example.com", Side.CPW, Role.EDITOR),
        AccountSeed("", null, Role.VIEWER, "WO"),
    )

    @Test
    fun `plan assigns ids to data rows only`() = runTest {
        val sheet = TemplateFixture.create()
        val plan = SheetMigrator(sheet, Random(1)).plan("id", accounts)

        assertTrue(plan.canApply)
        val byKey = plan.tables.associateBy { it.layout.key }
        assertEquals(listOf(26, 27), byKey.getValue("budget_akad").assignments.map { it.row })
        assertEquals(listOf(81), byKey.getValue("budget_mahar").assignments.map { it.row })
        assertEquals(listOf(20, 21), byKey.getValue("checklist_pra_nikah").assignments.map { it.row })
        assertEquals(listOf(19, 20), byKey.getValue("checklist_pernikahan").assignments.map { it.row })
        assertEquals(listOf(5), byKey.getValue("medical").assignments.map { it.row })
        assertEquals(listOf(23), byKey.getValue("perawatan").assignments.map { it.row })
        assertEquals(listOf(3, 4), byKey.getValue("guest_cpp").assignments.map { it.row })
        assertEquals(2, plan.accountsToAdd.size)
        assertEquals(1, plan.accountsSkipped.size)
    }

    @Test
    fun `apply writes only column A and the config tab, and is complete afterwards`() = runTest {
        val sheet = TemplateFixture.create()
        val migrator = SheetMigrator(sheet, Random(2))
        val report = migrator.apply(migrator.plan("id", accounts))

        assertTrue(report.steps.joinToString("\n"), report.isSuccess)
        assertTrue(report.formulaDiffs.isEmpty())

        val budget = sheet.tabs.getValue(Tabs.BUDGET)
        assertEquals(ID_HEADER, budget.get(24, 'A').asText())
        assertTrue(RowIds.isValid(budget.get(26, 'A').asText()))
        assertTrue(budget.get(28, 'A').isBlank) // reserved empty row: no id
        assertTrue(0 in budget.hiddenColumns)
        assertTrue(ID_PROTECTION_DESCRIPTION in budget.protectedDescriptions)
        assertEquals(CURRENT_SCHEMA_VERSION.toString(), sheet.developerMetadata[SCHEMA_VERSION_KEY])

        val config = sheet.tabs.getValue(Tabs.APP_CONFIG)
        assertTrue(config.hidden)
        assertEquals("owner@example.com", config.get(2, 'A').asText())
        assertEquals("CPW", config.get(3, 'B').asText())
        assertTrue(config.get(4, 'A').isBlank) // blank email skipped
    }

    @Test
    fun `second run is a no-op`() = runTest {
        val sheet = TemplateFixture.create()
        val migrator = SheetMigrator(sheet, Random(3))
        migrator.apply(migrator.plan("id", accounts))
        val idsAfterFirst = sheet.tabs.getValue(Tabs.GUEST_CPW).get(3, 'A')

        val second = migrator.plan("id", accounts)
        assertTrue(second.isComplete)
        assertEquals(0, second.idsToAssign)

        migrator.apply(second)
        assertEquals(idsAfterFirst, sheet.tabs.getValue(Tabs.GUEST_CPW).get(3, 'A'))
        assertEquals(1, sheet.tabs.getValue(Tabs.BUDGET).protectedDescriptions.size)
    }

    @Test
    fun `header mismatch blocks apply`() = runTest {
        val sheet = TemplateFixture.create()
        sheet.tabs.getValue(Tabs.BUDGET).text(24, 'E', "Vendor Name")
        val plan = SheetMigrator(sheet).plan("id", accounts)

        assertFalse(plan.canApply)
        val akad = plan.tables.single { it.layout.key == "budget_akad" }
        assertTrue(akad.mismatches.single().contains("E24"))
    }

    @Test
    fun `foreign data in column A blocks apply`() = runTest {
        val sheet = TemplateFixture.create()
        sheet.tabs.getValue(Tabs.GUEST_CPP).text(4, 'A', "catatan")
        val plan = SheetMigrator(sheet).plan("id", accounts)

        assertFalse(plan.canApply)
        assertTrue(plan.tables.single { it.layout.key == "guest_cpp" }.blockers.single().contains("A4"))
    }

    @Test
    fun `duplicate ids from a copied row are replaced`() = runTest {
        val sheet = TemplateFixture.create()
        val migrator = SheetMigrator(sheet, Random(4))
        migrator.apply(migrator.plan("id", accounts))
        val guests = sheet.tabs.getValue(Tabs.GUEST_CPP)
        guests.set(4, 'A', guests.get(3, 'A')) // user copy-pasted row 3 over row 4's id

        val plan = migrator.plan("id", accounts)
        val assignment = plan.tables.single { it.layout.key == "guest_cpp" }.assignments.single()
        assertEquals(4, assignment.row)
        assertTrue(assignment.replacesDuplicate)
    }

    @Test
    fun `missing tab prevents apply`() = runTest {
        val sheet = TemplateFixture.create()
        sheet.tabs.remove(Tabs.VENDOR)
        val plan = SheetMigrator(sheet).plan("id", accounts)
        assertEquals(listOf(Tabs.VENDOR), plan.missingTabs)
        assertFalse(plan.canApply)
    }
}
