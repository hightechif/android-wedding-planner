package app.weddingplanner.di

import android.content.Context
import app.weddingplanner.BuildConfig
import app.weddingplanner.core.auth.Emulator
import app.weddingplanner.core.auth.LocalDataMode
import app.weddingplanner.core.auth.WebClientId
import app.weddingplanner.core.datastore.DefaultSheetId
import app.weddingplanner.core.model.WeddingProfile
import app.weddingplanner.core.sheets.SheetsGateway
import app.weddingplanner.core.sheets.api.AccessTokenProvider
import app.weddingplanner.core.sheets.api.RetrofitSheetsGateway
import app.weddingplanner.core.sheets.api.SheetsClientFactory
import app.weddingplanner.core.sheets.layout.SheetInspector
import app.weddingplanner.core.sheets.local.LocalChangeStore
import app.weddingplanner.core.sheets.local.LocalSheetsGateway
import app.weddingplanner.core.sheets.migration.SheetMigrator
import app.weddingplanner.profile.WeddingProfileDefaults
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.io.File
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides
    fun provideProfile(): WeddingProfile = WeddingProfileDefaults.profile

    @Provides
    @WebClientId
    fun provideWebClientId(): String = BuildConfig.GOOGLE_WEB_CLIENT_ID

    @Provides
    @DefaultSheetId
    fun provideDefaultSheetId(local: LocalDataMode): String =
        if (local.enabled) LocalDataMode.SHEET_ID else BuildConfig.DEFAULT_SHEET_ID

    /** On by default for dev builds on an emulator that have local data bundled. */
    @Provides
    @Singleton
    fun provideLocalDataMode(@ApplicationContext context: Context): LocalDataMode = LocalDataMode(
        BuildConfig.FLAVOR == "dev" && Emulator.isEmulator() && LOCAL_XLSX in context.assets.list("local-sheet").orEmpty(),
    )

    @Provides
    @Singleton
    fun provideSheetsGateway(
        @ApplicationContext context: Context,
        local: LocalDataMode,
        tokens: AccessTokenProvider,
    ): SheetsGateway {
        if (local.enabled) {
            // dummy.xlsx, copied into the dev assets at build time and read as-is.
            // Changes made on the emulator (migration, row IDs) are kept in a private file across restarts.
            val changes = File(context.filesDir, "local-sheet-changes.json")
            val store = object : LocalChangeStore {
                override fun load() = changes.takeIf { it.exists() }?.readText()
                override fun save(json: String) = changes.writeText(json)
            }
            return context.assets.open("local-sheet/$LOCAL_XLSX").use {
                LocalSheetsGateway.fromXlsx(LocalDataMode.SHEET_ID, LocalDataMode.SHEET_TITLE, it, store)
            }
        }
        return RetrofitSheetsGateway(SheetsClientFactory.create(tokens, logBasic = BuildConfig.DEBUG))
    }

    private const val LOCAL_XLSX = "local.xlsx"

    @Provides
    fun provideInspector(gateway: SheetsGateway) = SheetInspector(gateway)

    @Provides
    fun provideMigrator(gateway: SheetsGateway) = SheetMigrator(gateway)
}
