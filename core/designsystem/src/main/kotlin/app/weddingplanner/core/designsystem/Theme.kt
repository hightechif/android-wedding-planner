package app.weddingplanner.core.designsystem

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Palette from docs/04-DESIGN-BRIEF.md §2 (sage #6B803F + cream #FFFDE7).
private val LightColors = lightColorScheme(
    primary = Color(0xFF56692E),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFD9E8B5),
    onPrimaryContainer = Color(0xFF1A2600),
    secondary = Color(0xFF6E5D3E),
    secondaryContainer = Color(0xFFF3E2C0),
    tertiary = Color(0xFF8A6D1F),
    tertiaryContainer = Color(0xFFFCE7A8),
    background = Color(0xFFFFFDE7),
    surface = Color(0xFFFFFDE7),
    surfaceContainerLow = Color(0xFFFAF7DF),
    surfaceContainer = Color(0xFFF4F1D8),
    surfaceContainerHigh = Color(0xFFEEEBD2),
    onSurface = Color(0xFF1C1C14),
    onSurfaceVariant = Color(0xFF47483B),
    outline = Color(0xFF77786A),
    outlineVariant = Color(0xFFC8C7B5),
    error = Color(0xFFBA1A1A),
    errorContainer = Color(0xFFFFDAD6),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFB5C98A),
    onPrimary = Color(0xFF263500),
    primaryContainer = Color(0xFF3E5018),
    onPrimaryContainer = Color(0xFFD9E8B5),
    secondary = Color(0xFFD9C6A0),
    tertiary = Color(0xFFE5C46A),
    background = Color(0xFF14140F),
    surface = Color(0xFF14140F),
    surfaceContainerLow = Color(0xFF1C1C17),
    surfaceContainer = Color(0xFF21211B),
    surfaceContainerHigh = Color(0xFF2B2B25),
    onSurface = Color(0xFFE6E3D1),
    onSurfaceVariant = Color(0xFFC8C7B5),
    outlineVariant = Color(0xFF47483B),
    error = Color(0xFFFFB4AB),
)

/** Semantic colours that Material 3 has no slot for (status chips, side markers). */
@Immutable
data class WeddingColors(
    val brandSage: Color,
    val success: Color,
    val successContainer: Color,
    val warning: Color,
    val warningContainer: Color,
    val cpp: Color,
    val cpw: Color,
)

private val LightWedding = WeddingColors(
    brandSage = Color(0xFF6B803F),
    success = Color(0xFF11734B), successContainer = Color(0xFFD4EDBC),
    warning = Color(0xFF473821), warningContainer = Color(0xFFFFE5A0),
    cpp = Color(0xFF3E6497), cpw = Color(0xFFB4536E),
)

private val DarkWedding = WeddingColors(
    brandSage = Color(0xFF9DB36A),
    success = Color(0xFFB6E3A0), successContainer = Color(0xFF1E4D33),
    warning = Color(0xFFFFE08A), warningContainer = Color(0xFF5A4712),
    cpp = Color(0xFFA9C7FF), cpw = Color(0xFFFFB1C4),
)

val LocalWeddingColors = staticCompositionLocalOf { LightWedding }

// Fraunces / Plus Jakarta Sans are bundled in a later milestone; system serif/sans until then.
private val DisplayFamily = FontFamily.Serif
private val BodyFamily = FontFamily.SansSerif

private val WeddingTypography = Typography().run {
    copy(
        displayLarge = TextStyle(fontFamily = DisplayFamily, fontWeight = FontWeight.SemiBold, fontSize = 56.sp, lineHeight = 64.sp),
        headlineMedium = TextStyle(fontFamily = DisplayFamily, fontWeight = FontWeight.Medium, fontSize = 28.sp, lineHeight = 36.sp),
        titleLarge = titleLarge.copy(fontFamily = BodyFamily, fontWeight = FontWeight.SemiBold),
        titleMedium = titleMedium.copy(fontFamily = BodyFamily, fontWeight = FontWeight.SemiBold),
        bodyLarge = bodyLarge.copy(fontFamily = BodyFamily),
        bodyMedium = bodyMedium.copy(fontFamily = BodyFamily),
        labelLarge = labelLarge.copy(fontFamily = BodyFamily, fontWeight = FontWeight.SemiBold),
    )
}

/** Dynamic colour is intentionally off: the sage/cream identity matters more than wallpaper matching. */
@Composable
fun WeddingTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalWeddingColors provides if (darkTheme) DarkWedding else LightWedding) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkColors else LightColors,
            typography = WeddingTypography,
            content = content,
        )
    }
}

object WeddingTheme {
    val colors: WeddingColors @Composable get() = LocalWeddingColors.current
}
