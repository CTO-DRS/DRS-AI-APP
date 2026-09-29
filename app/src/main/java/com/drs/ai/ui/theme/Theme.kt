package com.drs.ai.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Typography

private val Ocean = Color(0xFF1565C0)
private val OceanLight = Color(0xFF5E92F3)
private val Teal = Color(0xFF00897B)
private val Amber = Color(0xFFFFB300)

private val LightColors = lightColorScheme(
    primary = Ocean,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD6E4FF),
    onPrimaryContainer = Color(0xFF001B3F),
    secondary = Teal,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFB2DFDB),
    onSecondaryContainer = Color(0xFF00201C),
    tertiary = Amber,
    onTertiary = Color.Black,
    background = Color(0xFFFAFCFF),
    onBackground = Color(0xFF191C20),
    surface = Color(0xFFFAFCFF),
    onSurface = Color(0xFF191C20),
    surfaceVariant = Color(0xFFDFE2EB),
    onSurfaceVariant = Color(0xFF43474E),
    outline = Color(0xFF73777F)
)

private val DarkColors = darkColorScheme(
    primary = OceanLight,
    onPrimary = Color(0xFF00306E),
    primaryContainer = Color(0xFF00478D),
    onPrimaryContainer = Color(0xFFD6E4FF),
    secondary = Color(0xFF80CBC4),
    onSecondary = Color(0xFF003731),
    secondaryContainer = Color(0xFF005048),
    onSecondaryContainer = Color(0xFFB2DFDB),
    tertiary = Amber,
    onTertiary = Color(0xFF3F2E00),
    background = Color(0xFF111318),
    onBackground = Color(0xFFE2E2E9),
    surface = Color(0xFF111318),
    onSurface = Color(0xFFE2E2E9),
    surfaceVariant = Color(0xFF43474E),
    onSurfaceVariant = Color(0xFFC3C6CF),
    outline = Color(0xFF8D9199)
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
    val type = Typography(
        bodyLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 16.sp, lineHeight = 24.sp, fontWeight = FontWeight.Normal),
        bodyMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 14.sp, lineHeight = 20.sp),
        titleLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 22.sp, lineHeight = 28.sp, fontWeight = FontWeight.SemiBold),
        titleMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 16.sp, lineHeight = 24.sp, fontWeight = FontWeight.SemiBold),
        labelMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.Medium)
    )
    MaterialTheme(colorScheme = colors, typography = type, content = content)
}
