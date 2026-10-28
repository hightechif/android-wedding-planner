package app.weddingplanner.core.sheets.local

import app.weddingplanner.core.sheets.CellValue
import app.weddingplanner.core.sheets.CellWrite
import app.weddingplanner.core.sheets.ValueRender
import app.weddingplanner.core.sheets.layout.SCHEMA_VERSION_KEY
import app.weddingplanner.core.sheets.layout.Tabs
import app.weddingplanner.core.sheets.migration.SheetMigrator
import app.weddingplanner.core.sheets.pull.SheetPuller
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class LocalSheetsGatewayTest {
    private val gateway = LocalSheetsGateway.fromXlsx("local-id", "dummy", miniXlsx().inputStream())

    @Test
    fun `reads typed cells, shared strings, formulas and full tab titles`() = runTest {
        val row = gateway.batchGet("local-id", listOf("'WEDDING BUDGET'!A3:E3"), ValueRender.UNFORMATTED).single().row(3)
        assertEquals(CellValue.Text("08123"), row['B'])
        assertEquals(CellValue.Bool(true), row['C'])
        assertEquals(CellValue.Number(1.5), row['D'])
        assertEquals(CellValue.Number(200000.0), row['E'])
        val formula = gateway.batchGet("local-id", listOf("'WEDDING BUDGET'!A3:E3"), ValueRender.FORMULA).single().row(3)
        assertEquals(CellValue.Formula("=D3*2"), formula['E'])
        val titles = gateway.getSpreadsheet("local-id").sheets.map { it.title }
        assertEquals(listOf(Tabs.BUDGET, Tabs.MEDICAL, Tabs.APP_CONFIG), titles)
    }

    @Test
    fun `an exported _APP_CONFIG tab implies a migrated sheet`() = runTest {
        assertEquals("1", gateway.getSpreadsheet("local-id").developerMetadata[SCHEMA_VERSION_KEY])
        assertTrue(gateway.getSpreadsheet("local-id").sheet(Tabs.APP_CONFIG)!!.hidden)
    }

    @Test
    fun `writes are kept in memory`() = runTest {
        gateway.writeValues("local-id", listOf(CellWrite("'WEDDING BUDGET'!A3", CellValue.Text("BUD-AAAAAAAA"))))
        val row = gateway.batchGet("local-id", listOf("'WEDDING BUDGET'!A3:B3"), ValueRender.UNFORMATTED).single().row(3)
        assertEquals(CellValue.Text("BUD-AAAAAAAA"), row['A'])
    }

    @Test
    fun `changes survive a restart until the xlsx changes`() = runTest {
        val store = object : LocalChangeStore {
            var saved: String? = null
            override fun load() = saved
            override fun save(json: String) { saved = json }
        }
        val first = LocalSheetsGateway.fromXlsx("local-id", "dummy", miniXlsx().inputStream(), store)
        first.writeValues("local-id", listOf(CellWrite("'WEDDING BUDGET'!A3", CellValue.Text("BUD-AAAAAAAA"))))
        first.batchUpdate("local-id", listOf(SheetMigratorJson.metadata("wp.test", "2")))

        val restarted = LocalSheetsGateway.fromXlsx("local-id", "dummy", miniXlsx().inputStream(), store)
        val row = restarted.batchGet("local-id", listOf("'WEDDING BUDGET'!A3:B3"), ValueRender.UNFORMATTED).single().row(3)
        assertEquals(CellValue.Text("BUD-AAAAAAAA"), row['A'])
        assertEquals("2", restarted.getSpreadsheet("local-id").developerMetadata["wp.test"])

        val otherXlsx = LocalSheetsGateway.fromXlsx("local-id", "dummy", miniXlsx(extra = true).inputStream(), store)
        val fresh = otherXlsx.batchGet("local-id", listOf("'WEDDING BUDGET'!A3:B3"), ValueRender.UNFORMATTED).single().row(3)
        assertEquals(CellValue.Empty, fresh['A'])
    }

    @Test
    fun `the real export can be migrated, survives a restart, and then pulls cleanly`() = runTest {
        val file = File("../../dummy.xlsx") // git-ignored; skipped on machines without it
        if (!file.exists()) return@runTest
        val store = object : LocalChangeStore {
            var saved: String? = null
            override fun load() = saved
            override fun save(json: String) { saved = json }
        }
        val local = file.inputStream().use { LocalSheetsGateway.fromXlsx("id", "dummy", it, store) }
        val migrator = SheetMigrator(local)
        val plan = migrator.plan("id", emptyList())
        if (!plan.isComplete) {
            assertTrue(plan.toString(), plan.canApply)
            assertTrue(migrator.apply(plan) {}.isSuccess)
        }

        val restarted = file.inputStream().use { LocalSheetsGateway.fromXlsx("id", "dummy", it, store) }
        val meta = restarted.getSpreadsheet("id")
        assertEquals("1", meta.developerMetadata[SCHEMA_VERSION_KEY])
        val snap = SheetPuller(restarted).pull("id", hasAppConfig = meta.sheet(Tabs.APP_CONFIG) != null)
        assertTrue(snap.issues.toString(), snap.issues.isEmpty())
        assertTrue(snap.budget.value!!.akad.all { it.id != null })
    }

    private object SheetMigratorJson {
        fun metadata(key: String, value: String) = kotlinx.serialization.json.buildJsonObject {
            put("createDeveloperMetadata", kotlinx.serialization.json.buildJsonObject {
                put("developerMetadata", kotlinx.serialization.json.buildJsonObject {
                    put("metadataKey", kotlinx.serialization.json.JsonPrimitive(key))
                    put("metadataValue", kotlinx.serialization.json.JsonPrimitive(value))
                })
            })
        }
    }

    private fun miniXlsx(extra: Boolean = false): ByteArray {
        val ns = "xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\""
        val rns = "xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\""
        val parts = mapOf(
            "xl/workbook.xml" to """<workbook $ns $rns><sheets>
                <sheet name="WEDDING BUDGET" sheetId="1" r:id="rId1"/>
                <sheet name="Checklist Medical Check up &amp; Pe" sheetId="2" r:id="rId2"/>
                <sheet state="hidden" name="_APP_CONFIG" sheetId="3" r:id="rId3"/>
                <sheet name="INSTRUKSI!" sheetId="4" r:id="rId3"/></sheets></workbook>""",
            "xl/_rels/workbook.xml.rels" to """<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
                <Relationship Id="rId1" Target="worksheets/sheet1.xml"/>
                <Relationship Id="rId2" Target="worksheets/sheet2.xml"/>
                <Relationship Id="rId3" Target="/xl/worksheets/sheet3.xml"/></Relationships>""",
            "xl/sharedStrings.xml" to """<sst $ns><si><t>08123</t></si><si><r><t>em</t></r><r><t>ail</t></r></si></sst>""",
            "xl/worksheets/sheet1.xml" to """<worksheet $ns><sheetData><row r="3">
                <c r="B3" t="s"><v>0</v></c><c r="C3" t="b"><v>1</v></c><c r="D3"><v>1.5</v></c>
                <c r="E3"><f>D3*2</f><v>200000</v></c><c r="AA3"><v>9</v></c></row></sheetData></worksheet>""",
            "xl/worksheets/sheet2.xml" to if (extra) """<worksheet $ns><sheetData><row r="1"><c r="A1"><v>1</v></c></row></sheetData></worksheet>""" else """<worksheet $ns><sheetData/></worksheet>""",
            "xl/worksheets/sheet3.xml" to """<worksheet $ns><sheetData><row r="1"><c r="A1" t="s"><v>1</v></c></row></sheetData></worksheet>""",
        )
        return ByteArrayOutputStream().also { bytes ->
            ZipOutputStream(bytes).use { zip ->
                parts.forEach { (name, xml) ->
                    zip.putNextEntry(ZipEntry(name))
                    zip.write(xml.toByteArray())
                    zip.closeEntry()
                }
            }
        }.toByteArray()
    }
}
