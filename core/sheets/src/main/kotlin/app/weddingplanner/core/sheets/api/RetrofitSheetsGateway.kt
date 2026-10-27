package app.weddingplanner.core.sheets.api

import app.weddingplanner.core.sheets.A1Range
import app.weddingplanner.core.sheets.CellValue
import app.weddingplanner.core.sheets.CellWrite
import app.weddingplanner.core.sheets.SheetInfo
import app.weddingplanner.core.sheets.SheetRow
import app.weddingplanner.core.sheets.SheetsException
import app.weddingplanner.core.sheets.SheetsGateway
import app.weddingplanner.core.sheets.SpreadsheetMeta
import app.weddingplanner.core.sheets.ValueRange
import app.weddingplanner.core.sheets.ValueRender
import kotlinx.serialization.json.JsonObject
import retrofit2.HttpException

class RetrofitSheetsGateway(private val api: SheetsApi) : SheetsGateway {

    override suspend fun getSpreadsheet(spreadsheetId: String): SpreadsheetMeta = call {
        val dto = api.getSpreadsheet(spreadsheetId, SheetsApi.META_FIELDS)
        SpreadsheetMeta(
            spreadsheetId = dto.spreadsheetId.ifEmpty { spreadsheetId },
            title = dto.properties?.title.orEmpty(),
            sheets = dto.sheets.map { sheet ->
                SheetInfo(
                    sheetId = sheet.properties.sheetId,
                    title = sheet.properties.title,
                    hidden = sheet.properties.hidden,
                    rowCount = sheet.properties.gridProperties?.rowCount ?: 0,
                    columnCount = sheet.properties.gridProperties?.columnCount ?: 0,
                    protectedRangeDescriptions = sheet.protectedRanges.mapNotNull { it.description },
                )
            },
            developerMetadata = dto.developerMetadata
                .filter { it.metadataKey != null }
                .associate { it.metadataKey!! to it.metadataValue.orEmpty() },
        )
    }

    override suspend fun batchGet(
        spreadsheetId: String,
        ranges: List<String>,
        render: ValueRender,
    ): List<ValueRange> = call {
        if (ranges.isEmpty()) return@call emptyList()
        api.batchGet(spreadsheetId, ranges, render.apiValue).valueRanges.map { vr ->
            ValueRange(
                range = A1Range.parse(vr.range),
                rows = vr.values.map { row ->
                    SheetRow(row.map { CellValue.fromJson(it, formulaRender = render == ValueRender.FORMULA) })
                },
            )
        }
    }

    override suspend fun writeValues(spreadsheetId: String, writes: List<CellWrite>) = call {
        if (writes.isEmpty()) return@call
        api.valuesBatchUpdate(
            spreadsheetId,
            ValuesBatchUpdateDto(
                valueInputOption = "RAW",
                data = writes.map { ValueRangeDto(range = it.range, values = listOf(listOf(it.value.toJson()))) },
            ),
        )
        Unit
    }

    override suspend fun batchUpdate(spreadsheetId: String, requests: List<JsonObject>) = call {
        if (requests.isEmpty()) return@call
        api.batchUpdate(spreadsheetId, BatchUpdateDto(requests))
        Unit
    }

    private inline fun <T> call(block: () -> T): T = try {
        block()
    } catch (e: HttpException) {
        val body = e.response()?.errorBody()?.string().orEmpty()
        val googleMessage = GOOGLE_MESSAGE.find(body)?.groupValues?.get(1)
        throw SheetsException(e.code(), "Sheets API ${e.code()}: ${body.take(300)}", e, googleMessage)
    }

    private companion object {
        val GOOGLE_MESSAGE = Regex("\"message\"\\s*:\\s*\"((?:[^\"\\\\]|\\\\.)*)\"")
    }
}
