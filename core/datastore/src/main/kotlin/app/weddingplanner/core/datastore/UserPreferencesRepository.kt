package app.weddingplanner.core.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

data class UserPreferences(
    val accountEmail: String?,
    val linkedSpreadsheetId: String?,
    val linkedSpreadsheetTitle: String?,
) {
    val isSignedIn: Boolean get() = !accountEmail.isNullOrBlank()
    val isLinked: Boolean get() = isSignedIn && !linkedSpreadsheetId.isNullOrBlank()
}

/** Small per-device state (UserPrefs in docs/05-DATA-SCHEMA.md §4). Nothing here is a secret. */
@Singleton
class UserPreferencesRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) {
    val preferences: Flow<UserPreferences> = dataStore.data.map { p ->
        UserPreferences(
            accountEmail = p[ACCOUNT_EMAIL],
            linkedSpreadsheetId = p[SPREADSHEET_ID],
            linkedSpreadsheetTitle = p[SPREADSHEET_TITLE],
        )
    }

    suspend fun current(): UserPreferences = preferences.first()

    suspend fun setAccount(email: String) {
        dataStore.edit { it[ACCOUNT_EMAIL] = email.trim().lowercase() }
    }

    suspend fun setLinkedSpreadsheet(id: String, title: String) {
        dataStore.edit {
            it[SPREADSHEET_ID] = id
            it[SPREADSHEET_TITLE] = title
        }
    }

    suspend fun clear() {
        dataStore.edit { it.clear() }
    }

    private companion object {
        val ACCOUNT_EMAIL = stringPreferencesKey("account_email")
        val SPREADSHEET_ID = stringPreferencesKey("linked_spreadsheet_id")
        val SPREADSHEET_TITLE = stringPreferencesKey("linked_spreadsheet_title")
    }
}

private val Context.userPrefsDataStore by preferencesDataStore(name = "user_prefs")

@Module
@InstallIn(SingletonComponent::class)
object DataStoreModule {
    @Provides
    @Singleton
    fun provideDataStore(@ApplicationContext context: Context): DataStore<Preferences> = context.userPrefsDataStore
}

/** Sheet ID prefilled on the link screen: `BuildConfig.DEFAULT_SHEET_ID` of the current flavour. */
@javax.inject.Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class DefaultSheetId
