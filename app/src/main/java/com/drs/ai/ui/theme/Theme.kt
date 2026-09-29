package com.drs.ai.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Typography

// ─────────────────────────────────────────────────────────────────────────────
// DRS AI — brand palette v1.2 ("Nova Indigo")
// Deep indigo + violet core with electric cyan accents, on calm neutral surfaces.
// ─────────────────────────────────────────────────────────────────────────────
private val Indigo = Color(0xFF4F5BD5)
private val IndigoDeep = Color(0xFF3A45A8)
private val Violet = Color(0xFF7C4DFF)
private val Cyan = Color(0xFF22D3EE)
private val CyanDeep = Color(0xFF0E7490)
private val Amber = Color(0xFFFFB74D)

private val LightColors = lightColorScheme(
    primary = IndigoDeep,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE1E5FF),
    onPrimaryContainer = Color(0xFF0D1440),
    secondary = CyanDeep,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFC8F4FF),
    onSecondaryContainer = Color(0xFF062E36),
    tertiary = Violet,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFEBDCFF),
    onTertiaryContainer = Color(0xFF2A0A52),
    background = Color(0xFFF7F8FD),
    onBackground = Color(0xFF171A2B),
    surface = Color(0xFFFDFDFF),
    onSurface = Color(0xFF171A2B),
    surfaceVariant = Color(0xFFE4E6F4),
    onSurfaceVariant = Color(0xFF474A61),
    surfaceContainer = Color(0xFFF0F1FA),
    surfaceContainerHigh = Color(0xFFEAECF7),
    surfaceContainerHighest = Color(0xFFE4E6F4),
    outline = Color(0xFF787B93),
    outlineVariant = Color(0xFFC8CADB),
    error = Color(0xFFBA1A1A),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFBCC2FF),
    onPrimary = Color(0xFF1D2570),
    primaryContainer = Color(0xFF39439E),
    onPrimaryContainer = Color(0xFFE1E5FF),
    secondary = Color(0xFF67D8F0),
    onSecondary = Color(0xFF003544),
    secondaryContainer = Color(0xFF004D61),
    onSecondaryContainer = Color(0xFFC8F4FF),
    tertiary = Color(0xFFD3BCFF),
    onTertiary = Color(0xFF3D1A6E),
    tertiaryContainer = Color(0xFF553688),
    onTertiaryContainer = Color(0xFFEBDCFF),
    background = Color(0xFF0E1020),
    onBackground = Color(0xFFE3E4F2),
    surface = Color(0xFF141628),
    onSurface = Color(0xFFE3E4F2),
    surfaceVariant = Color(0xFF474A61),
    onSurfaceVariant = Color(0xFFC8CADB),
    surfaceContainer = Color(0xFF1A1D31),
    surfaceContainerHigh = Color(0xFF232640),
    surfaceContainerHighest = Color(0xFF2C3050),
    outline = Color(0xFF9294AD),
    outlineVariant = Color(0xFF474A61),
    error = Color(0xFFFFB4AB),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6)
)

// ─────────────────────────────────────────────────────────────────────────────
// Typography — clean, confident, generous line-heights for Arabic + Latin
// ─────────────────────────────────────────────────────────────────────────────
private val AppTypography = Typography(
    displaySmall = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 34.sp, lineHeight = 42.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.25).sp),
    headlineMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 26.sp, lineHeight = 34.sp, fontWeight = FontWeight.Bold),
    headlineSmall = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 22.sp, lineHeight = 30.sp, fontWeight = FontWeight.SemiBold),
    titleLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 21.sp, lineHeight = 28.sp, fontWeight = FontWeight.SemiBold),
    titleMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 16.sp, lineHeight = 24.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.1.sp),
    titleSmall = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.Medium),
    bodyLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 16.sp, lineHeight = 25.sp, fontWeight = FontWeight.Normal, letterSpacing = 0.2.sp),
    bodyMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 14.sp, lineHeight = 21.sp, fontWeight = FontWeight.Normal),
    bodySmall = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 12.sp, lineHeight = 17.sp, fontWeight = FontWeight.Normal),
    labelLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.SemiBold),
    labelMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.4.sp),
    labelSmall = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 11.sp, lineHeight = 14.sp, fontWeight = FontWeight.Medium, letterSpacing = 0.4.sp)
)

// ─────────────────────────────────────────────────────────────────────────────
// Shapes — softer, contemporary rounding
// ─────────────────────────────────────────────────────────────────────────────
private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun DrsTheme(
    themeMode: Int = 0, // 0 system, 1 light, 2 dark
    dynamicColors: Boolean = true,
    languageTag: String = "system",
    content: @Composable () -> Unit
) {
    val dark = when (themeMode) {
        1 -> false
        2 -> true
        else -> isSystemInDarkTheme()
    }
    val context = LocalContext.current
    val colors = when {
        dynamicColors && Build.VERSION.SDK_INT >= 31 ->
            if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        dark -> DarkColors
        else -> LightColors
    }
    MaterialTheme(
        colorScheme = colors,
        typography = AppTypography,
        shapes = AppShapes,
        content = content
    )
}
