package app.weddingplanner.feature.home

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import app.weddingplanner.core.datastore.UserPreferencesRepository
import app.weddingplanner.core.designsystem.SectionCard
import app.weddingplanner.core.designsystem.WeddingTheme
import app.weddingplanner.core.model.WeddingProfile
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import javax.inject.Inject

@Serializable data object HomeRoute

fun NavGraphBuilder.homeScreen(onOpenSheetSetup: (() -> Unit)?, onSignedOut: () -> Unit) {
    composable<HomeRoute> { HomeScreen(onOpenSheetSetup, onSignedOut) }
}

data class HomeUiState(
    val title: String,
    val daysToWedding: Long,
    val sheetTitle: String?,
    val sheetId: String?,
    val email: String?,
)

/** M0 placeholder Beranda: countdown from the profile and the linked sheet. Real data arrives in M1. */
@HiltViewModel
class HomeViewModel @Inject constructor(
    profile: WeddingProfile,
    preferences: UserPreferencesRepository,
    private val auth: GoogleAuthRepository,
) : ViewModel() {
    private val days = ChronoUnit.DAYS.between(LocalDate.now(ZoneId.of(profile.timeZone)), profile.weddingDate)

    val state: StateFlow<HomeUiState> = preferences.preferences
        .map { HomeUiState(profile.displayTitle, days, it.linkedSpreadsheetTitle, it.linkedSpreadsheetId, it.accountEmail) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState(profile.displayTitle, days, null, null, null))

    fun signOut(onDone: () -> Unit) {
        viewModelScope.launch {
            auth.signOut()
            onDone()
        }
    }
}

@Composable
internal fun HomeScreen(
    onOpenSheetSetup: (() -> Unit)?,
    onSignedOut: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(state.title, style = MaterialTheme.typography.titleLarge)
        CountdownHero(state.daysToWedding)

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

        SectionCard(title = "Segera hadir") {
            Text(
                "Ringkasan pembayaran, jatuh tempo, dan checklist muncul setelah sinkronisasi (M1).",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        if (onOpenSheetSetup != null) {
            OutlinedButton(onClick = onOpenSheetSetup, modifier = Modifier.fillMaxWidth()) { Text("Penyiapan Sheet (dev)") }
        }
        TextButton(onClick = { viewModel.signOut(onSignedOut) }, modifier = Modifier.fillMaxWidth()) { Text("Keluar") }
    }
}

@Composable
private fun CountdownHero(days: Long) {
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
    }
}
