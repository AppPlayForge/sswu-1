package com.example.myTools.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.platform.LocalContext

enum class AppThemeScheme {
    DYNAMIC, OCEAN, PRAIRIE, ORANGE, PINK, PURPLE, MONOCHROME, EARTH, BLUE
}

enum class DarkModeConfig {
    FOLLOW_SYSTEM, LIGHT, DARK
}

/**
 * 精細偏光處理：根據主色調生成背景與容器色
 */
private fun generatePolarizedScheme(
    primary: Color,
    secondary: Color,
    tertiary: Color,
    isDark: Boolean
): ColorScheme {
    return if (isDark) {
        // 深色模式：更明顯的主色調背景
        val darkBg = Color(0xFF0C0E11).compositeOver(primary.copy(alpha = 0.12f))
        val darkSurface = Color(0xFF1A1C1E).compositeOver(primary.copy(alpha = 0.15f))
        val darkOnSurface = Color(0xFFE2E2E6).compositeOver(primary.copy(alpha = 0.1f))
        
        darkColorScheme(
            primary = primary,
            onPrimary = Color.Black,
            primaryContainer = primary.copy(alpha = 0.25f).compositeOver(darkSurface),
            onPrimaryContainer = primary,
            secondary = secondary,
            onSecondary = Color.Black,
            secondaryContainer = secondary.copy(alpha = 0.25f).compositeOver(darkSurface),
            onSecondaryContainer = secondary,
            tertiary = tertiary,
            onTertiary = Color.Black,
            tertiaryContainer = tertiary.copy(alpha = 0.25f).compositeOver(darkSurface),
            onTertiaryContainer = tertiary,
            background = darkBg,
            onBackground = darkOnSurface,
            surface = darkSurface,
            onSurface = darkOnSurface,
            surfaceVariant = darkSurface.compositeOver(Color.White.copy(alpha = 0.08f)),
            onSurfaceVariant = darkOnSurface.copy(alpha = 0.75f),
            outline = Color(0xFF8D9199),
            surfaceContainer = darkSurface.compositeOver(primary.copy(alpha = 0.08f)),
            surfaceContainerLow = darkBg,
            surfaceContainerHigh = darkSurface.compositeOver(primary.copy(alpha = 0.16f)),
            surfaceContainerHighest = darkSurface.compositeOver(primary.copy(alpha = 0.22f))
        )
    } else {
        // 淺色模式：更有「色彩感」的背景 (參考 Pixel 設置頁面)
        val lightBg = Color(0xFFFDFCFF).compositeOver(primary.copy(alpha = 0.06f))
        val lightSurface = Color(0xFFFDFCFF).compositeOver(primary.copy(alpha = 0.08f))
        val lightOnSurface = Color(0xFF1A1C1E).compositeOver(primary.copy(alpha = 0.1f))
        
        lightColorScheme(
            primary = primary,
            onPrimary = Color.White,
            primaryContainer = primary.copy(alpha = 0.22f).compositeOver(lightSurface),
            onPrimaryContainer = primary,
            secondary = secondary,
            onSecondary = Color.White,
            secondaryContainer = secondary.copy(alpha = 0.22f).compositeOver(lightSurface),
            onSecondaryContainer = secondary,
            tertiary = tertiary,
            onTertiary = Color.White,
            tertiaryContainer = tertiary.copy(alpha = 0.22f).compositeOver(lightSurface),
            onTertiaryContainer = tertiary,
            background = lightBg,
            onBackground = lightOnSurface,
            surface = lightSurface,
            onSurface = lightOnSurface,
            surfaceVariant = Color(0xFFDFE2EB).compositeOver(primary.copy(alpha = 0.05f)),
            onSurfaceVariant = lightOnSurface.copy(alpha = 0.75f),
            outline = Color(0xFF73777F),
            surfaceContainer = lightSurface,
            surfaceContainerLow = lightBg,
            surfaceContainerHigh = lightSurface.compositeOver(primary.copy(alpha = 0.08f)),
            surfaceContainerHighest = lightSurface.compositeOver(primary.copy(alpha = 0.14f))
        )
    }
}

private fun getAppColorScheme(scheme: AppThemeScheme, isDark: Boolean): ColorScheme {
    val (p, s, t) = when(scheme) {
        AppThemeScheme.OCEAN -> Triple(ocean_primary, ocean_secondary, ocean_tertiary)
        AppThemeScheme.PRAIRIE -> Triple(prairie_primary, prairie_secondary, prairie_tertiary)
        AppThemeScheme.ORANGE -> Triple(orange_primary, orange_secondary, orange_tertiary)
        AppThemeScheme.PINK -> Triple(pink_primary, pink_secondary, pink_tertiary)
        AppThemeScheme.PURPLE -> Triple(purple_primary, purple_secondary, purple_tertiary)
        AppThemeScheme.MONOCHROME -> Triple(monochrome_primary, monochrome_secondary, monochrome_tertiary)
        AppThemeScheme.EARTH -> Triple(earth_primary, earth_secondary, earth_tertiary)
        AppThemeScheme.BLUE, AppThemeScheme.DYNAMIC -> Triple(blue_primary, blue_secondary, blue_tertiary)
    }
    return generatePolarizedScheme(p, s, t, isDark)
}

@Composable
fun RulerTheme(
    themeScheme: AppThemeScheme = AppThemeScheme.DYNAMIC,
    darkModeConfig: DarkModeConfig = DarkModeConfig.FOLLOW_SYSTEM,
    content: @Composable () -> Unit
) {
    val darkTheme = when (darkModeConfig) {
        DarkModeConfig.FOLLOW_SYSTEM -> isSystemInDarkTheme()
        DarkModeConfig.LIGHT -> false
        DarkModeConfig.DARK -> true
    }

    val colorScheme = when {
        themeScheme == AppThemeScheme.DYNAMIC && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> getAppColorScheme(themeScheme, true)
        else -> getAppColorScheme(themeScheme, false)
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
