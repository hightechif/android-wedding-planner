package app.weddingplanner.core.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.text.NumberFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private val ID = Locale.forLanguageTag("id-ID")
private val GROUPING = NumberFormat.getIntegerInstance(ID)
private val DATE = DateTimeFormatter.ofPattern("d MMM yyyy", ID)

/** `Rp1.000.000` with Indonesian grouping (TRD §5.9); negative as `-Rp1.000`. */
fun formatRupiah(amount: Long): String = (if (amount < 0) "-Rp" else "Rp") + GROUPING.format(kotlin.math.abs(amount))

/** `22%` from a 0.0–1.0 fraction. */
fun formatPercent(fraction: Double): String = "${Math.round(fraction * 100)}%"

fun formatDate(date: LocalDate): String = DATE.format(date)

/** Label on the left, value on the right. */
@Composable
fun MetricRow(label: String, value: String, valueColor: Color = Color.Unspecified, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth()) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = valueColor)
    }
}

enum class ChipTone { SUCCESS, WARNING, NEUTRAL, ERROR }

/** Small status chip; colour plus text, never colour alone (Design Brief §6). */
@Composable
fun StatusChip(text: String, tone: ChipTone, modifier: Modifier = Modifier) {
    val colors = WeddingTheme.colors
    val (bg, fg) = when (tone) {
        ChipTone.SUCCESS -> colors.successContainer to colors.success
        ChipTone.WARNING -> colors.warningContainer to colors.warning
        ChipTone.ERROR -> MaterialTheme.colorScheme.errorContainer to MaterialTheme.colorScheme.error
        ChipTone.NEUTRAL -> MaterialTheme.colorScheme.surfaceContainerHigh to MaterialTheme.colorScheme.onSurfaceVariant
    }
    Text(
        text,
        style = MaterialTheme.typography.labelMedium,
        color = fg,
        modifier = modifier.background(bg, RoundedCornerShape(8.dp)).padding(horizontal = 8.dp, vertical = 2.dp),
    )
}
