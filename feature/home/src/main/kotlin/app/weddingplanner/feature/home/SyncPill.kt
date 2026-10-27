package app.weddingplanner.feature.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.weddingplanner.core.designsystem.CheckRow
import app.weddingplanner.core.designsystem.CheckState
import app.weddingplanner.core.designsystem.SectionCard
import app.weddingplanner.core.designsystem.WeddingTheme
import app.weddingplanner.core.model.SyncState
import app.weddingplanner.core.model.SyncStatus
import kotlinx.coroutines.delay
import java.time.Duration
import java.time.Instant

/** SET-1 in compact form: state + last synced time. Tap to sync now. */
@Composable
internal fun SyncPill(status: SyncStatus, onClick: () -> Unit) {
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(30_000)
            now = System.currentTimeMillis()
        }
    }
    val colors = WeddingTheme.colors
    val (label, bg, fg) = when (status.state) {
        SyncState.SYNCING -> Triple("Menyinkronkan…", MaterialTheme.colorScheme.surfaceContainerHigh, MaterialTheme.colorScheme.onSurfaceVariant)
        SyncState.OFFLINE -> Triple("Offline", MaterialTheme.colorScheme.surfaceContainerHigh, MaterialTheme.colorScheme.onSurfaceVariant)
        SyncState.AUTH_REQUIRED -> Triple("Perlu masuk ulang", MaterialTheme.colorScheme.errorContainer, MaterialTheme.colorScheme.error)
        SyncState.ERROR -> Triple("Gagal sinkron", MaterialTheme.colorScheme.errorContainer, MaterialTheme.colorScheme.error)
        SyncState.SCHEMA_MISMATCH -> Triple("Cek layout sheet", colors.warningContainer, colors.warning)
        SyncState.IDLE -> Triple(status.lastSuccessAt?.let { "Tersinkron ${ago(it, now)}" } ?: "Belum sinkron", colors.successContainer, colors.success)
    }
    Surface(color = bg, contentColor = fg, shape = RoundedCornerShape(50), modifier = Modifier.clickable(onClick = onClick)) {
        Text(label, style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp))
    }
}

/** Layout mismatches and sync errors, so the user knows which module stopped updating. */
@Composable
internal fun SyncProblems(status: SyncStatus) {
    if (status.mismatches.isEmpty() && status.message == null) return
    SectionCard(title = null) {
        status.message?.let {
            val state = if (status.state == SyncState.IDLE) CheckState.OK else CheckState.WARNING
            CheckRow(it, state)
        }
        status.mismatches.forEach { CheckRow("Layout berbeda — modul ini tidak diperbarui", CheckState.ERROR, it) }
    }
}

private fun ago(at: Instant, nowMillis: Long): String {
    val minutes = Duration.between(at, Instant.ofEpochMilli(nowMillis)).toMinutes()
    return when {
        minutes < 1 -> "baru saja"
        minutes < 60 -> "$minutes mnt lalu"
        minutes < 24 * 60 -> "${minutes / 60} jam lalu"
        else -> "${minutes / (24 * 60)} hari lalu"
    }
}
