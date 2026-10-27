package app.weddingplanner.feature.sheetsetup

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import app.weddingplanner.core.datastore.UserPreferencesRepository
import app.weddingplanner.core.designsystem.CheckRow
import app.weddingplanner.core.designsystem.CheckState
import app.weddingplanner.core.designsystem.SectionCard
import app.weddingplanner.core.model.WeddingProfile
import app.weddingplanner.core.sheets.migration.MigrationPlan
import app.weddingplanner.core.sheets.migration.MigrationReport
import app.weddingplanner.core.sheets.migration.SheetMigrator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import javax.inject.Inject

@Serializable data object SheetSetupRoute

fun NavGraphBuilder.sheetSetupScreen(onBack: () -> Unit) {
    composable<SheetSetupRoute> { SheetSetupScreen(onBack) }
}

data class SheetSetupUiState(
    val busy: Boolean = false,
    val plan: MigrationPlan? = null,
    val log: List<String> = emptyList(),
    val report: MigrationReport? = null,
    val error: String? = null,
)

/** Debug-only "Penyiapan Sheet" (M0): preview, then run the idempotent migration on the linked sheet. */
@HiltViewModel
class SheetSetupViewModel @Inject constructor(
    private val migrator: SheetMigrator,
    private val preferences: UserPreferencesRepository,
    private val profile: WeddingProfile,
) : ViewModel() {
    private val _state = MutableStateFlow(SheetSetupUiState())
    val state: StateFlow<SheetSetupUiState> = _state.asStateFlow()

    init { preview() }

    fun preview() = launchBusy {
        val id = requireNotNull(preferences.current().linkedSpreadsheetId) { "Belum ada sheet yang terhubung." }
        val plan = migrator.plan(id, profile.accounts)
        _state.update { it.copy(plan = plan, report = null, log = emptyList()) }
    }

    fun run() = launchBusy {
        val plan = requireNotNull(_state.value.plan)
        val report = migrator.apply(plan) { step -> _state.update { it.copy(log = it.log + step) } }
        _state.update { it.copy(report = report, plan = report.verification) }
    }

    private fun launchBusy(block: suspend () -> Unit) {
        _state.update { it.copy(busy = true, error = null) }
        viewModelScope.launch {
            try {
                block()
            } catch (e: Exception) {
                _state.update { it.copy(error = e.message ?: e.javaClass.simpleName) }
            } finally {
                _state.update { it.copy(busy = false) }
            }
        }
    }
}

@Composable
internal fun SheetSetupScreen(onBack: () -> Unit, viewModel: SheetSetupViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var confirm by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        TextButton(onClick = onBack) { Text("← Kembali") }
        Text("Penyiapan Sheet", style = MaterialTheme.typography.headlineMedium)
        Text(
            "Menambahkan kolom _id tersembunyi (kolom A), skema versi, proteksi peringatan, dan tab _APP_CONFIG. " +
                "Jalankan dulu di salinan uji. Aman dijalankan berulang.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (state.busy) CircularProgressIndicator(Modifier.size(28.dp))
        state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        state.plan?.let { PlanCard(it) }
        if (state.log.isNotEmpty()) {
            SectionCard(title = "Log") { state.log.forEach { Text(it, style = MaterialTheme.typography.bodySmall) } }
        }
        state.report?.let { ReportCard(it) }

        val plan = state.plan
        OutlinedButton(onClick = viewModel::preview, enabled = !state.busy, modifier = Modifier.fillMaxWidth()) {
            Text("Periksa ulang")
        }
        Button(
            onClick = { confirm = true },
            enabled = !state.busy && plan != null && plan.canApply && !plan.isComplete,
            modifier = Modifier.fillMaxWidth().height(52.dp),
        ) { Text("Jalankan migrasi") }
    }

    if (confirm) {
        AlertDialog(
            onDismissRequest = { confirm = false },
            title = { Text("Ubah sheet ini?") },
            text = { Text("Migrasi akan menulis ke \"${state.plan?.title}\". Pastikan ini salinan uji, atau kamu sudah mengujinya di sana.") },
            confirmButton = {
                TextButton(onClick = {
                    confirm = false
                    viewModel.run()
                }) { Text("Jalankan") }
            },
            dismissButton = { TextButton(onClick = { confirm = false }) { Text("Batal") } },
        )
    }
}

@Composable
private fun PlanCard(plan: MigrationPlan) {
    SectionCard(title = plan.title) {
        plan.missingTabs.forEach { CheckRow("Tab $it", CheckState.ERROR, "Tidak ditemukan") }
        plan.tables.forEach { t ->
            val state = when {
                !t.isSafe -> CheckState.ERROR
                t.assignments.isNotEmpty() || t.headerNeedsWrite -> CheckState.WARNING
                else -> CheckState.OK
            }
            val detail = (t.mismatches + t.blockers).firstOrNull()
                ?: "${t.dataRows} baris data · ${t.existingIds} ID ada · ${t.assignments.size} ID baru"
            CheckRow(t.layout.key, state, detail)
        }
        CheckRow(
            "Skema versi",
            if (plan.schemaVersion != null) CheckState.OK else CheckState.WARNING,
            plan.schemaVersion?.let { "v$it" } ?: "Akan dibuat (wp.schemaVersion = 1)",
        )
        CheckRow(
            "Proteksi kolom A",
            if (plan.unprotectedTabs.isEmpty()) CheckState.OK else CheckState.WARNING,
            if (plan.unprotectedTabs.isEmpty()) null else "Akan ditambahkan di ${plan.unprotectedTabs.size} tab",
        )
        CheckRow(
            "_APP_CONFIG",
            if (plan.appConfigExists && plan.accountsToAdd.isEmpty()) CheckState.OK else CheckState.WARNING,
            buildString {
                append(if (plan.appConfigExists) "${plan.appConfigRowCount} akun" else "Tab akan dibuat")
                if (plan.accountsToAdd.isNotEmpty()) append(" · +${plan.accountsToAdd.size} akun")
                if (plan.accountsSkipped.isNotEmpty()) append(" · ${plan.accountsSkipped.size} dilewati (email kosong di wedding-profile.json)")
            },
        )
    }
}

@Composable
private fun ReportCard(report: MigrationReport) {
    SectionCard(title = if (report.isSuccess) "Migrasi selesai ✓" else "Migrasi perlu dicek") {
        Text("ID baru: ${report.idsAssigned.values.sum()}", style = MaterialTheme.typography.bodyMedium)
        if (report.formulaDiffs.isEmpty()) {
            CheckRow("Rumus di luar kolom A tidak berubah", CheckState.OK)
        } else {
            CheckRow("${report.formulaDiffs.size} sel berubah", CheckState.ERROR, report.formulaDiffs.take(5).joinToString("\n"))
        }
    }
}
