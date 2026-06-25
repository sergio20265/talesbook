package ru.norahobbits.talesbook.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import ru.norahobbits.talesbook.settings.AppTheme

private fun TalesbookPalette.toColorScheme() = darkColorScheme(
    background = background,
    surface = surface,
    surfaceVariant = surfaceSoft,
    primary = accent,
    onPrimary = background,
    onBackground = textPrimary,
    onSurface = textPrimary,
    onSurfaceVariant = textSecondary,
    secondary = accentSoft,
    onSecondary = textPrimary,
    error = danger,
    outline = accentSoft
)

@Composable
fun TalesbookTheme(
    appTheme: AppTheme = AppTheme.EVENING_FOREST,
    content: @Composable () -> Unit
) {
    val palette = when (appTheme) {
        AppTheme.EVENING_FOREST -> EveningForestPalette
        AppTheme.WITCH_LIBRARY -> WitchLibraryPalette
        AppTheme.MOON_GARDEN -> MoonGardenPalette
        AppTheme.HOBBIT_ROOM -> HobbitRoomPalette
    }

    CompositionLocalProvider(LocalTalesbookColors provides palette) {
        MaterialTheme(
            colorScheme = palette.toColorScheme(),
            typography = TalesbookTypography,
            content = content
        )
    }
}
