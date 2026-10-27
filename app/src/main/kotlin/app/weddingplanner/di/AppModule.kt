package app.weddingplanner.di

import app.weddingplanner.BuildConfig
import app.weddingplanner.core.auth.WebClientId
import app.weddingplanner.core.datastore.DefaultSheetId
import app.weddingplanner.core.model.WeddingProfile
import app.weddingplanner.core.sheets.SheetsGateway
import app.weddingplanner.core.sheets.api.AccessTokenProvider
import app.weddingplanner.core.sheets.api.RetrofitSheetsGateway
import app.weddingplanner.core.sheets.api.SheetsClientFactory
import app.weddingplanner.core.sheets.layout.SheetInspector
import app.weddingplanner.core.sheets.migration.SheetMigrator
import app.weddingplanner.profile.WeddingProfileDefaults
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
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
    fun provideDefaultSheetId(): String = BuildConfig.DEFAULT_SHEET_ID

    @Provides
    @Singleton
    fun provideSheetsGateway(tokens: AccessTokenProvider): SheetsGateway =
        RetrofitSheetsGateway(SheetsClientFactory.create(tokens, logBasic = BuildConfig.DEBUG))

    @Provides
    fun provideInspector(gateway: SheetsGateway) = SheetInspector(gateway)

    @Provides
    fun provideMigrator(gateway: SheetsGateway) = SheetMigrator(gateway)
}
