package app.weddingplanner.core.sheets.api

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

/** Google Sheets API v4, only the endpoints listed in TRD §3.3. */
interface SheetsApi {
    @GET("v4/spreadsheets/{id}")
    suspend fun getSpreadsheet(
        @Path("id") spreadsheetId: String,
        @Query("fields") fields: String,
    ): SpreadsheetDto

    @GET("v4/spreadsheets/{id}/values:batchGet")
    suspend fun batchGet(
        @Path("id") spreadsheetId: String,
        @Query("ranges") ranges: List<String>,
        @Query("valueRenderOption") valueRenderOption: String,
        @Query("dateTimeRenderOption") dateTimeRenderOption: String = "SERIAL_NUMBER",
        @Query("majorDimension") majorDimension: String = "ROWS",
    ): BatchGetDto

    @POST("v4/spreadsheets/{id}/values:batchUpdate")
    suspend fun valuesBatchUpdate(
        @Path("id") spreadsheetId: String,
        @Body body: ValuesBatchUpdateDto,
    ): JsonObject

    @POST("v4/spreadsheets/{id}:batchUpdate")
    suspend fun batchUpdate(
        @Path("id") spreadsheetId: String,
        @Body body: BatchUpdateDto,
    ): JsonObject

    companion object {
        const val BASE_URL = "https://sheets.googleapis.com/"
        const val META_FIELDS =
            "spreadsheetId,properties(title)," +
                "sheets(properties(sheetId,title,hidden,gridProperties(rowCount,columnCount)),protectedRanges(description))," +
                "developerMetadata(metadataKey,metadataValue)"
    }
}

@Serializable
data class SpreadsheetDto(
    val spreadsheetId: String = "",
    val properties: SpreadsheetPropertiesDto? = null,
    val sheets: List<SheetDto> = emptyList(),
    val developerMetadata: List<DeveloperMetadataDto> = emptyList(),
)

@Serializable
data class SpreadsheetPropertiesDto(val title: String = "")

@Serializable
data class SheetDto(
    val properties: SheetPropertiesDto,
    val protectedRanges: List<ProtectedRangeDto> = emptyList(),
)

@Serializable
data class SheetPropertiesDto(
    val sheetId: Long = 0,
    val title: String = "",
    val hidden: Boolean = false,
    val gridProperties: GridPropertiesDto? = null,
)

@Serializable
data class GridPropertiesDto(val rowCount: Int = 0, val columnCount: Int = 0)

@Serializable
data class ProtectedRangeDto(val description: String? = null)

@Serializable
data class DeveloperMetadataDto(val metadataKey: String? = null, val metadataValue: String? = null)

@Serializable
data class BatchGetDto(val valueRanges: List<ValueRangeDto> = emptyList())

@Serializable
data class ValueRangeDto(
    val range: String,
    val majorDimension: String? = null,
    val values: List<List<JsonElement>> = emptyList(),
)

@Serializable
data class ValuesBatchUpdateDto(
    val valueInputOption: String,
    val data: List<ValueRangeDto>,
)

@Serializable
data class BatchUpdateDto(val requests: List<JsonObject>)
