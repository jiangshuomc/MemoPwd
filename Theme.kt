package com.example.memopwd.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

val BrandBlue = Color(0xFF4FC3F7)
val BrandSlate = Color(0xFF546E7A)
val BrandDeep = Color(0xFF0A0A1A)
val BrandSurface = Color(0xFF141428)
val BrandDim = Color(0xFF37474F)

private val AppColorScheme = darkColorScheme(
    primary = BrandBlue,
    onPrimary = BrandDeep,
    secondary = BrandSlate,
    background = BrandDeep,
    onBackground = Color(0xFFE6E6F0),
    surface = BrandSurface,
    onSurface = Color(0xFFE6E6F0),
    surfaceVariant = Color(0xFF1E1E36),
    onSurfaceVariant = Color(0xFFA8B0C0),
    outline = Color(0xFF2E2E48),
    error = Color(0xFFFF6B6B)
)

@Composable
fun MemoPwdTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            dynamicDarkColorScheme(LocalContext.current)
        else -> AppColorScheme
    }

    val view = LocalView.current
    if (!view.isInDesignMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            window.navigationBarColor = colorScheme.background.toArgb()
            val controller = WindowCompat.getInsetsController(window, view)
            controller.isAppearanceLightStatusBars = false
            controller.isAppearanceLightNavigationBars = false
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        content = content
    )
}
