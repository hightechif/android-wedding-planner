package app.weddingplanner.core.sheets.local

import app.weddingplanner.core.sheets.CellValue
import org.w3c.dom.Element
import java.io.InputStream
import java.util.zip.ZipInputStream
import javax.xml.parsers.DocumentBuilderFactory

/** One tab of an .xlsx file: cached cell values plus formula text, keyed by (row, column letter). */
class XlsxTab(
    val title: String,
    val hidden: Boolean,
    val values: Map<Pair<Int, Char>, CellValue>,
    val formulas: Map<Pair<Int, Char>, String>,
)

/**
 * Reads an .xlsx (e.g. Google Sheets → File → Download → .xlsx) with the JDK only: it's a zip of
 * XML parts. Values are the cached results, like `UNFORMATTED_VALUE` (dates stay serial numbers).
 * Columns past Z are ignored, as the app never reads them. Shared-formula children (Excel stores
 * only the first cell's text) come back without formula text.
 */
object XlsxReader {
    private const val REL_NS = "http://schemas.openxmlformats.org/officeDocument/2006/relationships"

    // Excel truncates tab titles to 31 characters; map them back to the Google Sheets titles.
    private val FULL_TITLES = mapOf("Checklist Medical Check up & Pe" to "Checklist Medical Check up & Perawatan")

    fun read(input: InputStream): List<XlsxTab> {
        val parts = mutableMapOf<String, ByteArray>()
        ZipInputStream(input).use { zip ->
            generateSequence { zip.nextEntry }.forEach { e ->
                if (e.name.endsWith(".xml") || e.name.endsWith(".rels")) parts[e.name] = zip.readBytes()
            }
        }
        val factory = DocumentBuilderFactory.newInstance().apply { isNamespaceAware = true }
        fun xml(name: String): Element? = parts[name]?.let { factory.newDocumentBuilder().parse(it.inputStream()).documentElement }

        val shared = xml("xl/sharedStrings.xml")?.children("si")?.map(::textOf).orEmpty()
        val rels = xml("xl/_rels/workbook.xml.rels")?.children("Relationship")
            ?.associate { it.getAttribute("Id") to it.getAttribute("Target") }.orEmpty()
        val sheets = xml("xl/workbook.xml")?.children("sheets")?.firstOrNull()?.children("sheet").orEmpty()

        return sheets.mapNotNull { sheet ->
            val target = rels[sheet.getAttributeNS(REL_NS, "id")]?.removePrefix("/") ?: return@mapNotNull null
            val doc = xml(if (target.startsWith("xl/")) target else "xl/$target") ?: return@mapNotNull null
            val values = mutableMapOf<Pair<Int, Char>, CellValue>()
            val formulas = mutableMapOf<Pair<Int, Char>, String>()
            for (c in doc.descendants("c")) {
                val ref = c.getAttribute("r")
                val letters = ref.takeWhile(Char::isLetter)
                if (letters.length != 1) continue
                val key = ref.drop(1).toInt() to letters.single()
                c.children("f").firstOrNull()?.textContent?.takeIf { it.isNotEmpty() }?.let { formulas[key] = "=$it" }
                val v = c.children("v").firstOrNull()?.textContent.orEmpty()
                val cell: CellValue = when (c.getAttribute("t")) {
                    "s" -> v.toIntOrNull()?.let { shared.getOrNull(it) }?.let(CellValue::Text) ?: CellValue.Empty
                    "inlineStr" -> CellValue.Text(textOf(c))
                    "str" -> CellValue.Text(v)
                    "b" -> CellValue.Bool(v == "1")
                    "e" -> CellValue.Empty
                    else -> v.toDoubleOrNull()?.let(CellValue::Number) ?: CellValue.Empty
                }
                if (!cell.isBlank) values[key] = cell
            }
            val title = sheet.getAttribute("name")
            XlsxTab(FULL_TITLES[title] ?: title, sheet.getAttribute("state") == "hidden", values, formulas)
        }
    }

    private fun Element.children(tag: String): List<Element> =
        (0 until childNodes.length).map { childNodes.item(it) }.filterIsInstance<Element>().filter { it.localName == tag }

    private fun Element.descendants(tag: String): List<Element> =
        getElementsByTagNameNS("*", tag).let { list -> (0 until list.length).map { list.item(it) as Element } }

    private fun textOf(e: Element): String = e.descendants("t").joinToString("") { it.textContent }
}
