package com.example.trackwell.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = NeonCyan,
    secondary = ElectricViolet,
    tertiary = EmeraldGreen,
    background = DeepObsidian,
    surface = CardDark,
    onPrimary = DeepObsidian,
    onSecondary = TextPrimaryDark,
    onTertiary = DeepObsidian,
    onBackground = TextPrimaryDark,
    onSurface = TextPrimaryDark,
    error = SunsetOrange
)

private val LightColorScheme = lightColorScheme(
    primary = ElectricViolet,
    secondary = NeonCyan,
    tertiary = EmeraldGreen,
    background = LightBg,
    surface = LightCard,
    onPrimary = TextPrimaryDark,
    onSecondary = TextPrimaryLight,
    onTertiary = TextPrimaryDark,
    onBackground = TextPrimaryLight,
    onSurface = TextPrimaryLight,
    error = SunsetOrange
)

@Composable
fun TrackWellTheme(
    darkTheme: Boolean = true, // Force premium dark theme by default
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
