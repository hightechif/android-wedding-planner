package app.weddingplanner.feature.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import app.weddingplanner.core.auth.AuthRequiredException
import app.weddingplanner.core.datastore.DefaultSheetId
import app.weddingplanner.core.datastore.UserPreferencesRepository
import app.weddingplanner.core.designsystem.CheckRow
import app.weddingplanner.core.designsystem.CheckState
import app.weddingplanner.core.designsystem.SectionCard
import app.weddingplanner.core.sheets.SheetsException
import app.weddingplanner.core.sheets.SpreadsheetIdParser
import app.weddingplanner.core.sheets.layout.InspectionReport
import app.weddingplanner.core.sheets.layout.SheetInspector
import app.weddingplanner.core.sheets.layout.Tabs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.IOException
import javax.inject.Inject

data class LinkSheetUiState(
    val input: String,
    val checking: Boolean = false,
    val spreadsheetId: String? = null,
    val report: InspectionReport? = null,
    val error: String? = null,
    val linked: Boolean = false,
)

@HiltViewModel
class LinkSheetViewModel @Inject constructor(
    @DefaultSheetId defaultSheetId: String,
    private val inspector: SheetInspector,
    private val preferences: UserPreferencesRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(LinkSheetUiState(input = defaultSheetId))
    val state: StateFlow<LinkSheetUiState> = _state.asStateFlow()

    fun onInputChange(value: String) = _state.update { it.copy(input = value, report = null, error = null) }

    fun check() {
        val id = SpreadsheetIdParser.parse(_state.value.input)
        if (id == null) {
            _state.update { it.copy(error = "ID atau URL sheet tidak valid.") }
            return
        }
        _state.update { it.copy(checking = true, error = null, report = null) }
        viewModelScope.launch {
            val error = try {
                val report = inspector.inspect(id)
                _state.update { it.copy(checking = false, spreadsheetId = id, report = report) }
                null
            } catch (e: SheetsException) {
                when {
                    e.isApiDisabled -> "Google Sheets API belum diaktifkan di project Google Cloud aplikasi ini."
                    e.isScopeMissing -> "Izin akses Sheets belum diberikan. Keluar, lalu masuk lagi dan centang izin Sheets."
                    e.isNotGoogleSheet -> "File ini berformat Excel (.xlsx), bukan Google Sheets. Buka file, lalu File → Simpan sebagai Google Spreadsheet."
                    e.isNoAccess || e.isNotFound -> "Akun ini belum punya akses ke sheet. Minta pemilik sheet untuk membagikannya."
                    else -> "Gagal membaca sheet (${e.httpCode})."
                } + e.googleMessage?.let { "\n\nDetail dari Google: $it" }.orEmpty()
            } catch (e: AuthRequiredException) {
                e.message
            } catch (e: IOException) {
                "Tidak ada koneksi. Coba lagi."
            }
            if (error != null) _state.update { it.copy(checking = false, error = error) }
        }
    }

    fun link() {
        val s = _state.value
        val id = s.spreadsheetId ?: return
        val report = s.report?.takeIf { it.canLink } ?: return
        viewModelScope.launch {
            preferences.setLinkedSpreadsheet(id, report.title)
            _state.update { it.copy(linked = true) }
        }
    }
}

@Composable
internal fun LinkSheetScreen(onLinked: () -> Unit, viewModel: LinkSheetViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(state.linked) { if (state.linked) onLinked() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("Hubungkan Sheet", style = MaterialTheme.typography.headlineMedium)
        Text(
            "Tempel ID atau URL Google Sheet pernikahan kalian.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        OutlinedTextField(
            value = state.input,
            onValueChange = viewModel::onInputChange,
            label = { Text("ID atau URL sheet") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedButton(
            onClick = viewModel::check,
            enabled = !state.checking && state.input.isNotBlank(),
            modifier = Modifier.fillMaxWidth().height(48.dp),
        ) {
            if (state.checking) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp) else Text("Periksa sheet")
        }
        state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        state.report?.let { report ->
            InspectionCard(report)
            Spacer(Modifier.height(4.dp))
            Button(
                onClick = viewModel::link,
                enabled = report.canLink,
                modifier = Modifier.fillMaxWidth().height(52.dp),
            ) { Text("Hubungkan") }
        }
    }
}

@Composable
private fun InspectionCard(report: InspectionReport) {
    SectionCard(title = report.title) {
        Tabs.REQUIRED.forEach { tab ->
            val missing = tab in report.missingTabs
            CheckRow(tab, if (missing) CheckState.ERROR else CheckState.OK, if (missing) "Tab tidak ditemukan" else null)
        }
        report.tables.forEach { check ->
            CheckRow(
                label = "Tata letak ${check.layout.key}",
                state = if (check.isValid) CheckState.OK else CheckState.WARNING,
                detail = check.mismatches.firstOrNull()?.let { "$it (modul ini akan hanya-baca)" },
            )
        }
        CheckRow(
            label = if (report.isMigrated) "Sheet sudah disiapkan (skema v${report.schemaVersion})" else "Sheet belum disiapkan (M0)",
            state = if (report.isMigrated) CheckState.OK else CheckState.WARNING,
            detail = if (report.isMigrated) null else "Di build dev, jalankan Penyiapan Sheet dari Beranda. Di build prod, hubungi pemilik sheet.",
        )
    }
}
