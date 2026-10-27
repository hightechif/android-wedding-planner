package app.weddingplanner.core.sheets

import app.weddingplanner.core.sheets.layout.Tabs

/** Builds a minimal copy of the wedding template with the headers and a few rows per table. */
object TemplateFixture {
    fun create(): FakeSheetsGateway = FakeSheetsGateway().apply {
        tab(Tabs.DASHBOARD).set(10, 'O', CellValue.Number(46404.0))
        tab(Tabs.VENDOR).text(5, 'B', "Kategori")

        tab(Tabs.BUDGET).apply {
            listOf('B' to "No", 'C' to "Category", 'D' to "Detail Item", 'E' to "Vendor", 'J' to "Sudah Dibayar", 'N' to "Jatuh Tempo")
                .forEach { (c, t) -> text(24, c, t) }
            text(77, 'B', "MAHAR & SESERAHAN")
            listOf('B' to "No", 'C' to "Category", 'D' to "Detailed Item", 'E' to "Groom / Bride", 'J' to "Sudah Dibayar", 'N' to "Jatuh Tempo")
                .forEach { (c, t) -> text(79, c, t) }
            budgetRow(26, "Administrasi", "Dokumen")
            budgetRow(27, "Catering", "Catering")
            // row 28 stays empty but carries the template formulas (reserved row)
            set(28, 'I', CellValue.Formula("=H28*F28"))
            budgetRow(81, "Mahar", "Cincin")
        }

        listOf(Tabs.PRA_NIKAH to 16, Tabs.PERNIKAHAN to 15).forEach { (title, header) ->
            tab(title).apply {
                listOf('B' to "NO", 'C' to "ITEM", 'H' to "STATUS", 'J' to "DETAIL / KETERANGAN", 'K' to "PIC")
                    .forEach { (c, t) -> text(header, c, t) }
                text(header + 2, 'B', "1")
                text(header + 2, 'C', "SECTION")
                text(header + 2, 'H', "CPP")
                text(header + 3, 'D', "Sub-section")
                checklistItem(header + 4, "Item satu", true, false)
                checklistItem(header + 5, "Item dua", false, false)
            }
        }

        tab(Tabs.MEDICAL).apply {
            text(4, 'B', "Nama Pemeriksaan"); text(4, 'E', "Check")
            text(5, 'B', "Golongan Darah"); set(5, 'E', CellValue.Bool(false))
            text(22, 'B', "Waktu"); text(22, 'C', "Kegiatan"); text(22, 'E', "Check")
            text(23, 'B', "H-6 Bulan"); text(23, 'C', "Olahraga"); set(23, 'E', CellValue.Bool(false))
        }

        listOf(Tabs.GUEST_CPP, Tabs.GUEST_CPW).forEach { title ->
            tab(title).apply {
                listOf('B' to "No.", 'C' to "Kategori", 'E' to "Nama", 'H' to "Hadir").forEach { (c, t) -> text(2, c, t) }
                set(3, 'B', CellValue.Formula("=ARRAYFORMULA(...)"))
                text(3, 'C', "Keluarga"); text(3, 'E', "Tamu A"); text(3, 'H', "Ya")
                text(4, 'C', "Teman"); text(4, 'E', "Tamu B")
                text(180, 'B', "TOTAL COUNT \"YA\"")
                set(180, 'F', CellValue.Formula("=COUNTIF(F3:F178, \"Ya\")"))
            }
        }
    }

    private fun FakeSheetsGateway.Tab.budgetRow(row: Int, category: String, detail: String) {
        text(row, 'C', category)
        text(row, 'D', detail)
        set(row, 'F', CellValue.Number(1.0))
        set(row, 'H', CellValue.Number(100_000.0))
        set(row, 'I', CellValue.Formula("=H$row*F$row"))
    }

    private fun FakeSheetsGateway.Tab.checklistItem(row: Int, text: String, cpp: Boolean, cpw: Boolean) {
        text(row, 'E', text)
        set(row, 'H', CellValue.Bool(cpp))
        set(row, 'I', CellValue.Bool(cpw))
    }
}
