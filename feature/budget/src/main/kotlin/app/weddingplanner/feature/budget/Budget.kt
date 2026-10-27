package app.weddingplanner.feature.budget

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import app.weddingplanner.core.data.WeddingRepository
import app.weddingplanner.core.designsystem.ChipTone
import app.weddingplanner.core.designsystem.MetricRow
import app.weddingplanner.core.designsystem.SectionCard
import app.weddingplanner.core.designsystem.StatusChip
import app.weddingplanner.core.designsystem.WeddingTheme
import app.weddingplanner.core.designsystem.formatDate
import app.weddingplanner.core.designsystem.formatPercent
import app.weddingplanner.core.designsystem.formatRupiah
import app.weddingplanner.core.model.BudgetItem
import app.weddingplanner.core.model.BudgetTable
import app.weddingplanner.core.model.PaymentStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.serialization.Serializable
import javax.inject.Inject

@Serializable data object BudgetListRoute
@Serializable data class BudgetItemRoute(val rowId: String)

fun NavGraphBuilder.budgetScreens(onOpenItem: (String) -> Unit, onBack: () -> Unit) {
    composable<BudgetListRoute> { BudgetListScreen(onOpenItem) }
    composable<BudgetItemRoute> { BudgetItemScreen(onBack) }
}

data class BudgetFilter(
    val table: BudgetTable = BudgetTable.AKAD_RESEPSI,
    val query: String = "",
    val category: String? = null,
    val status: PaymentStatus? = null,
)

data class BudgetListUiState(
    val filter: BudgetFilter = BudgetFilter(),
    val categories: List<String> = emptyList(),
    val items: List<BudgetItem> = emptyList(),
    val total: Long = 0,
    val remaining: Long = 0,
)

/** BUD-1, read-only in M1: two tables, search, category and status filters. */
@HiltViewModel
class BudgetListViewModel @Inject constructor(repository: WeddingRepository) : ViewModel() {
    private val filter = MutableStateFlow(BudgetFilter())

    val state: StateFlow<BudgetListUiState> = combine(repository.budgetItems, filter) { all, f ->
        val inTable = all.filter { it.table == f.table }
        val shown = inTable.filter { item ->
            (f.category == null || item.categoryRaw == f.category) &&
                (f.status == null || item.status == f.status) &&
                (f.query.isBlank() || listOfNotNull(item.detail, item.vendorOrPayer).any { it.contains(f.query.trim(), ignoreCase = true) })
        }
        BudgetListUiState(
            filter = f,
            categories = inTable.map { it.categoryRaw }.filter { it.isNotBlank() }.distinct().sorted(),
            items = shown,
            total = shown.sumOf { it.total },
            remaining = shown.sumOf { it.remaining ?: 0 },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), BudgetListUiState())

    fun selectTable(table: BudgetTable) = filter.update { BudgetFilter(table = table, query = it.query) }
    fun search(query: String) = filter.update { it.copy(query = query) }
    fun toggleCategory(category: String) = filter.update { it.copy(category = if (it.category == category) null else category) }
    fun toggleStatus(status: PaymentStatus) = filter.update { it.copy(status = if (it.status == status) null else status) }
}

@Composable
internal fun BudgetListScreen(onOpenItem: (String) -> Unit, viewModel: BudgetListViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val f = state.filter
    Column(Modifier.fillMaxSize()) {
        Text("Budget", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(16.dp))
        PrimaryTabRow(selectedTabIndex = f.table.ordinal) {
            Tab(selected = f.table == BudgetTable.AKAD_RESEPSI, onClick = { viewModel.selectTable(BudgetTable.AKAD_RESEPSI) }, text = { Text("Akad & Resepsi") })
            Tab(selected = f.table == BudgetTable.MAHAR_SESERAHAN, onClick = { viewModel.selectTable(BudgetTable.MAHAR_SESERAHAN) }, text = { Text("Mahar & Seserahan") })
        }
        OutlinedTextField(
            value = f.query,
            onValueChange = viewModel::search,
            placeholder = { Text(if (f.table == BudgetTable.AKAD_RESEPSI) "Cari item atau vendor" else "Cari item") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        )
        Row(
            Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            PaymentStatus.entries.forEach { s ->
                FilterChip(selected = f.status == s, onClick = { viewModel.toggleStatus(s) }, label = { Text(s.sheetValue) })
            }
            state.categories.forEach { c ->
                FilterChip(selected = f.category == c, onClick = { viewModel.toggleCategory(c) }, label = { Text(c) })
            }
        }
        Row(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
            Text(
                "${state.items.size} item · Total ${formatRupiah(state.total)} · Sisa ${formatRupiah(state.remaining)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        LazyColumn(contentPadding = PaddingValues(bottom = 16.dp)) {
            items(state.items, key = { it.rowId }) { item ->
                BudgetRow(item, onClick = { onOpenItem(item.rowId) })
                HorizontalDivider(Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant)
            }
            if (state.items.isEmpty()) {
                item {
                    Text(
                        "Tidak ada item yang cocok.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun BudgetRow(item: BudgetItem, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(item.detail, style = MaterialTheme.typography.bodyLarge)
            Text(
                listOfNotNull(item.categoryRaw.ifBlank { null }, item.vendorOrPayer).joinToString(" · "),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (item.isPriceMissing) {
                Text("Harga belum diisi", style = MaterialTheme.typography.bodySmall, color = WeddingTheme.colors.warning)
            }
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(formatRupiah(item.total), style = MaterialTheme.typography.bodyMedium)
            item.remaining?.takeIf { it > 0 }?.let {
                Text("Sisa ${formatRupiah(it)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            StatusChip(item)
        }
    }
}

@Composable
private fun StatusChip(item: BudgetItem) {
    val raw = item.statusRaw ?: return
    val tone = when (item.status) {
        PaymentStatus.LUNAS -> ChipTone.SUCCESS
        PaymentStatus.DP -> ChipTone.WARNING
        PaymentStatus.BELUM_BAYAR -> ChipTone.NEUTRAL
        null -> ChipTone.NEUTRAL
    }
    StatusChip(raw, tone, Modifier.padding(top = 4.dp))
}

/** BUD-2, read-only: every field plus the sheet's derived Total, Sisa, Progress and Status. */
@HiltViewModel
class BudgetItemViewModel @Inject constructor(savedState: SavedStateHandle, repository: WeddingRepository) : ViewModel() {
    private val rowId = savedState.toRoute<BudgetItemRoute>().rowId
    val item: StateFlow<BudgetItem?> = repository.budgetItem(rowId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
}

@Composable
internal fun BudgetItemScreen(onBack: () -> Unit, viewModel: BudgetItemViewModel = hiltViewModel()) {
    val item by viewModel.item.collectAsStateWithLifecycle()
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        TextButton(onClick = onBack) { Text("← Kembali") }
        val i = item ?: run {
            Text("Item tidak ditemukan. Mungkin sudah dihapus di sheet.", style = MaterialTheme.typography.bodyMedium)
            return@Column
        }
        Text(i.detail, style = MaterialTheme.typography.headlineMedium)
        StatusChip(i)
        SectionCard(title = "Pembayaran") {
            MetricRow("Total", formatRupiah(i.total))
            MetricRow("Sudah Dibayar", formatRupiah(i.paid), valueColor = WeddingTheme.colors.success)
            i.remaining?.let { MetricRow("Sisa", formatRupiah(it)) }
            i.progress?.let { MetricRow("Progress", formatPercent(it)) }
            MetricRow("Jatuh Tempo", i.dueDate?.let(::formatDate) ?: "—")
        }
        SectionCard(title = "Detail") {
            MetricRow("No", i.no?.toString() ?: "—")
            MetricRow("Kategori", i.categoryRaw.ifBlank { "—" })
            MetricRow(if (i.table == BudgetTable.AKAD_RESEPSI) "Vendor" else "Ditanggung", i.vendorOrPayer ?: "—")
            MetricRow("Qty", listOfNotNull(i.qty?.let(::formatQty), i.unit).joinToString(" ").ifBlank { "—" })
            MetricRow("Harga satuan", i.price?.let(::formatRupiah) ?: "Harga belum diisi")
            i.priceFormula?.let { MetricRow("Rumus harga", it) }
        }
        i.notes?.let { notes ->
            SectionCard(title = "Catatan") { Text(notes, style = MaterialTheme.typography.bodyMedium) }
        }
        Text(
            "Hanya baca. Mengubah item dan mencatat pembayaran hadir di M2.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private fun formatQty(q: Double): String = if (q % 1.0 == 0.0) q.toLong().toString() else q.toString().replace('.', ',')
