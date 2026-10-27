package app.weddingplanner.core.sheets

import kotlinx.serialization.json.JsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class PrimitivesTest {
    @Test
    fun `parses quoted and unquoted A1 ranges`() {
        assertEquals(A1Range("WEDDING BUDGET", 'A', 24, 'O', 106), A1Range.parse("'WEDDING BUDGET'!A24:O106"))
        assertEquals(A1Range("DASHBOARD", 'O', 10, 'O', 10), A1Range.parse("DASHBOARD!O10"))
        assertEquals(A1Range("It's", 'A', 1, 'K', null), A1Range.parse("'It''s'!A1:K"))
        assertEquals("'It''s'!A1:K", a1Range("It's", 'A', 1, 'K', null))
    }

    @Test
    fun `cell values keep their type`() {
        assertEquals(CellValue.Bool(true), CellValue.fromJson(JsonPrimitive(true)))
        assertEquals(CellValue.Number(46404.0), CellValue.fromJson(JsonPrimitive(46404)))
        assertEquals(CellValue.Empty, CellValue.fromJson(JsonPrimitive("")))
        assertEquals(CellValue.Text("=x"), CellValue.fromJson(JsonPrimitive("=x")))
        assertEquals(CellValue.Formula("=H26*F26"), CellValue.fromJson(JsonPrimitive("=H26*F26"), formulaRender = true))
        assertEquals("200000", CellValue.Number(200000.0).asText())
    }

    @Test
    fun `row ids are well formed and unique`() {
        val taken = mutableSetOf<String>()
        repeat(500) { taken += RowIds.generate("BUD", taken, Random(it)) }
        assertEquals(500, taken.size)
        assertTrue(taken.all(RowIds::isValid))
        assertFalse(RowIds.isValid("BUD-0000000I")) // I is not Crockford
    }

    @Test
    fun `spreadsheet id from url or raw`() {
        val id = "1AbCdEfGhIjKlMnOpQrStUvWxYz0123456789_-ab"
        assertEquals(id, SpreadsheetIdParser.parse("https://docs.google.com/spreadsheets/d/$id/edit#gid=0"))
        assertEquals(id, SpreadsheetIdParser.parse("  $id "))
        assertNull(SpreadsheetIdParser.parse("not a sheet"))
    }
}
