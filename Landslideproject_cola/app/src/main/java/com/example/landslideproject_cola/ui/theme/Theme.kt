package com.example.landslideproject_cola.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary    = Red80,
    secondary  = RedGrey80,
    tertiary   = Orange80,
    background = Color(0xFF1A1A2E),
    surface    = Color(0xFF16213E),
    onPrimary  = Color.White,
    onBackground = Color.White,
    onSurface  = Color.White
)

private val LightColorScheme = lightColorScheme(
    primary    = Red40,
    secondary  = RedGrey40,
    tertiary   = Orange40,
    background = Color(0xFFFAF4F4),
    surface    = Color(0xFFFFFFFF),
    onPrimary  = Color.White,
    onBackground = Color(0xFF1C1B1F),
    onSurface  = Color(0xFF1C1B1F)
)

@Composable
fun LandslideProjectTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,   // ปิด dynamic color เพื่อใช้ธีมแผ่นดินไหวคงที่
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = Color(0xFFB71C1C).toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}