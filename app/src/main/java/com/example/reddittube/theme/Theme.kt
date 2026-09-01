package com.lean.reddittube.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

import androidx.compose.ui.graphics.Color

private val BrandContainer = BrandRed.copy(alpha = 0.15f)

fun createDarkColorScheme(amoledMode: Boolean = true) = darkColorScheme(
    primary = Color.White,
    onPrimary = Color.Black,
    primaryContainer = BrandContainer,
    onPrimaryContainer = Color.White,
    secondary = TextSecondary,
    onSecondary = Color.White,
    background = Color.Black,
    onBackground = TextPrimary,
    surface = Color.Black,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceRaised,
    onSurfaceVariant = TextSecondary,
    outline = BorderSubtle,
    outlineVariant = GlassBorder
)

@Composable
fun RdTubeTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    amoledMode: Boolean = false,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        else -> createDarkColorScheme(amoledMode)
    }
    MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}

