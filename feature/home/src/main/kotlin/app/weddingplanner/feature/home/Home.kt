package app.weddingplanner.feature.home

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import app.weddingplanner.core.auth.GoogleAuthRepository
import app.weddingplanner.core.data.SyncEngine
import app.weddingplanner.core.data.SyncTrigger
import app.weddingplanner.core.data.WeddingRepository
import app.weddingplanner.core.datastore.UserPreferencesRepository
import app.weddingplanner.core.designsystem.MetricRow
import app.weddingplanner.core.designsystem.SectionCard
import app.weddingplanner.core.designsystem.WeddingTheme
import app.weddingplanner.core.designsystem.formatDate
import app.weddingplanner.core.designsystem.formatPercent
import app.weddingplanner.core.designsystem.formatRupiah
import app.weddingplanner.core.model.BudgetItem
import app.weddingplanner.core.model.BudgetSummary
import app.weddingplanner.core.model.EventType
import app.weddingplanner.core.model.PaymentStatus
import app.weddingplanner.core.model.SyncState
import app.weddingplanner.core.model.SyncStatus
import app.weddingplanner.core.model.WeddingEvent
import app.weddingplanner.core.model.WeddingProfile
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import javax.inject.Inject

@Serializable data object HomeRoute

fun NavGraphBuilder.homeScreen(
    onOpenBudgetItem: (String) -> Unit,
    onOpenSheetSetup: (() -> Unit)?,
    onSignedOut: () -> Unit,
) {
    composable<HomeRoute> { HomeScreen(onOpenBudgetItem, onOpenSheetSetup, onSignedOut) }
}

data class DueItem(val item: BudgetItem, val daysLeft: Long)

data class HomeUiState(
    val title: String,
    val daysToWedding: Long,
    val nextEvent: Pair<WeddingEvent, Long>? = null,
    val summary: BudgetSummary? = null,
    val due: List<DueItem> = emptyList(),
    val sync: SyncStatus = SyncStatus(),
    val sheetTitle: String? = null,
    val sheetId: String? = null,
    val email: String? = null,
)

private val EVENT_LABELS = mapOf(
    EventType.LAMARAN to "Lamaran",
    EventType.PENGAJIAN_SIRAMAN to "Pengajian/Siraman",
    EventType.PRE_WEDDING to "Pre Wedding",
    EventType.WEDDING to "Wedding",
)

/** Beranda (HOME-1/3/4/5): countdown and payment numbers straight from the synced sheet. */
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val profile: WeddingProfile,
    preferences: UserPreferencesRepository,
    private val repository: WeddingRepository,
    private val sync: SyncEngine,
    private val auth: GoogleAuthRepository,
) : ViewModel() {
    private val zone = ZoneId.of(profile.timeZone)

    val state: StateFlow<HomeUiState> = combine(
        repository.events,
        repository.budgetSummary,
        repository.budgetItems,
        sync.status,
        preferences.preferences,
    ) { events, summary, items, status, prefs ->
        val today = LocalDate.now(zone)
        val wedding = events.firstOrNull { it.type == EventType.WEDDING }?.date ?: profile.weddingDate
        HomeUiState(
            title = profile.displayTitle,
            daysToWedding = ChronoUnit.DAYS.between(today, wedding),
            nextEvent = events
                .filter { it.type != EventType.WEDDING && !it.date.isBefore(today) }
                .minByOrNull { it.date }
                ?.let { it to ChronoUnit.DAYS.between(today, it.date) },
            summary = summary,
            due = dueItems(items, today),
            sync = status,
            sheetTitle = prefs.linkedSpreadsheetTitle,
            sheetId = prefs.linkedSpreadsheetId,
            email = prefs.accountEmail,
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        HomeUiState(profile.displayTitle, ChronoUnit.DAYS.between(LocalDate.now(zone), profile.weddingDate)),
    )

    fun refresh() {
        viewModelScope.launch { sync.sync(SyncTrigger.MANUAL) }
    }

    fun signOut(onDone: () -> Unit) {
        viewModelScope.launch {
            auth.signOut()
            repository.clearLocalData()
            onDone()
        }
    }

    companion object {
        /** HOME-4: not Lunas, Jatuh Tempo within 14 days or already passed; overdue first. */
        fun dueItems(items: List<BudgetItem>, today: LocalDate): List<DueItem> = items
            .filter { it.total > 0 && it.status != PaymentStatus.LUNAS && it.dueDate != null }
            .map { DueItem(it, ChronoUnit.DAYS.between(today, it.dueDate)) }
            .filter { it.daysLeft <= 14 }
            .sortedBy { it.daysLeft }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun HomeScreen(
    onOpenBudgetItem: (String) -> Unit,
    onOpenSheetSetup: (() -> Unit)?,
    onSignedOut: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    PullToRefreshBox(
        isRefreshing = state.sync.state == SyncState.SYNCING,
        onRefresh = viewModel::refresh,
        modifier = Modifier.fillMaxSize(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(state.title, style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                SyncPill(state.sync, onClick = viewModel::refresh)
            }
            SyncProblems(state.sync)
            CountdownHero(state.daysToWedding, state.nextEvent)

            val summary = state.summary
            if (summary == null) {
                SectionCard(title = "Ringkasan Pembayaran") {
                    Text(
                        "Menunggu sinkronisasi pertama…",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                PaymentSummaryCard(summary)
                DueCard(state.due, onOpenBudgetItem)
                BudgetHealthCard(summary)
            }

            SectionCard(title = "Sheet terhubung") {
                Text(state.sheetTitle ?: "—", style = MaterialTheme.typography.bodyLarge)
                state.email?.let {
                    Text("Masuk sebagai $it", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                state.sheetId?.let { id ->
                    TextButton(onClick = {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://docs.google.com/spreadsheets/d/$id/edit")))
                    }) { Text("Buka di Google Sheets") }
                }
            }

            if (onOpenSheetSetup != null) {
                OutlinedButton(onClick = onOpenSheetSetup, modifier = Modifier.fillMaxWidth()) { Text("Penyiapan Sheet (dev)") }
            }
            TextButton(onClick = { viewModel.signOut(onSignedOut) }, modifier = Modifier.fillMaxWidth()) { Text("Keluar") }
        }
    }
}

@Composable
private fun CountdownHero(days: Long, nextEvent: Pair<WeddingEvent, Long>?) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(28.dp))
            .padding(24.dp),
    ) {
        when {
            days > 0 -> {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text("H-", style = MaterialTheme.typography.headlineMedium, color = WeddingTheme.colors.brandSage)
                    Text("$days", style = MaterialTheme.typography.displayLarge, color = MaterialTheme.colorScheme.tertiary)
                }
                Text("$days hari lagi menuju hari bahagia", style = MaterialTheme.typography.bodyLarge)
            }
            days == 0L -> Text("Hari ini!", style = MaterialTheme.typography.displayLarge, color = MaterialTheme.colorScheme.tertiary)
            else -> Text("Selamat menempuh hidup baru", style = MaterialTheme.typography.headlineMedium)
        }
        nextEvent?.let { (event, left) ->
            Text(
                "Berikutnya: ${EVENT_LABELS[event.type]} · ${formatDate(event.date)} · " + if (left == 0L) "hari ini" else "$left hari lagi",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }
}

@Composable
private fun PaymentSummaryCard(s: BudgetSummary) {
    SectionCard(title = "Ringkasan Pembayaran") {
        MetricRow("Total Biaya", formatRupiah(s.totalBiaya))
        MetricRow("Sudah Dibayar", formatRupiah(s.sudahDibayar), valueColor = WeddingTheme.colors.success)
        MetricRow("Sisa Pembayaran", formatRupiah(s.sisaPembayaran))
        LinearProgressIndicator(
            progress = { s.progress.toFloat().coerceIn(0f, 1f) },
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        )
        MetricRow("Progress", formatPercent(s.progress))
        if (s.itemLunas != null && s.itemTotal != null) {
            MetricRow("Item Lunas", "${s.itemLunas} dari ${s.itemTotal} item")
        }
    }
}

@Composable
private fun DueCard(due: List<DueItem>, onOpen: (String) -> Unit) {
    SectionCard(title = "Jatuh Tempo") {
        if (due.isEmpty()) {
            Text(
                "Tidak ada pembayaran jatuh tempo dalam 14 hari",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        due.forEach { d ->
            val overdue = d.daysLeft < 0
            Column(Modifier.fillMaxWidth().clickable { onOpen(d.item.rowId) }.padding(vertical = 4.dp)) {
                Text(d.item.detail, style = MaterialTheme.typography.bodyLarge)
                Text(
                    buildString {
                        append(formatDate(d.item.dueDate!!))
                        append(" · ")
                        append(
                            when {
                                overdue -> "lewat ${-d.daysLeft} hari"
                                d.daysLeft == 0L -> "hari ini"
                                else -> "${d.daysLeft} hari lagi"
                            },
                        )
                        append(" · Sisa ")
                        append(formatRupiah(d.item.remaining ?: (d.item.total - d.item.paid)))
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = if (overdue) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun BudgetHealthCard(s: BudgetSummary) {
    val over = s.selisih < 0
    SectionCard(title = if (over) "Budget terlampaui" else "Kesehatan Budget") {
        MetricRow("Budgeting Nikah", formatRupiah(s.budgetingNikah))
        MetricRow("Total Estimasi Budget", formatRupiah(s.totalEstimasi))
        MetricRow(
            "Selisih Budget",
            formatRupiah(s.selisih),
            valueColor = if (over) MaterialTheme.colorScheme.error else WeddingTheme.colors.success,
        )
        MetricRow("Budget terpakai", formatPercent(s.budgetTerpakai))
    }
}
