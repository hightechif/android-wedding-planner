package app.weddingplanner.feature.vendor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import app.weddingplanner.core.data.WeddingRepository
import app.weddingplanner.core.designsystem.ChipTone
import app.weddingplanner.core.designsystem.SectionCard
import app.weddingplanner.core.designsystem.StatusChip
import app.weddingplanner.core.model.FinalVendor
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.serialization.Serializable
import javax.inject.Inject

@Serializable data object VendorRoute

fun NavGraphBuilder.vendorScreen() {
    composable<VendorRoute> { VendorScreen() }
}

/** VEN-1, read-only: the Finalisasi Vendor block of `Perbandingan Vendor`. */
@HiltViewModel
class VendorViewModel @Inject constructor(repository: WeddingRepository) : ViewModel() {
    val vendors: StateFlow<List<FinalVendor>> = repository.finalVendors
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
}

@Composable
internal fun VendorScreen(viewModel: VendorViewModel = hiltViewModel()) {
    val vendors by viewModel.vendors.collectAsStateWithLifecycle()
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Vendor", style = MaterialTheme.typography.headlineMedium)
        Text(
            "${vendors.count { it.vendorName != null }} dari ${vendors.size} kategori sudah punya vendor",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        vendors.forEach { VendorCard(it) }
    }
}

@Composable
private fun VendorCard(v: FinalVendor) {
    SectionCard(title = null) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(v.category, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(v.vendorName ?: "Belum dipilih", style = MaterialTheme.typography.titleMedium)
            }
            v.paymentRaw?.let { p ->
                StatusChip(p, if (p.equals("Lunas", ignoreCase = true)) ChipTone.SUCCESS else ChipTone.WARNING)
            }
        }
        v.meetingRaw?.let { m ->
            Text("Pertemuan: $m", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        v.notes?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
    }
}
