package ru.norahobbits.talesbook.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

@Immutable
data class TalesbookPalette(
    val background: Color,
    val surface: Color,
    val surfaceSoft: Color,
    val surfaceElevated: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textHint: Color,
    val accent: Color,
    val accentSoft: Color,
    val accentDim: Color,
    val danger: Color = Color(0xFF9E4E43)
)

val EveningForestPalette = TalesbookPalette(
    background = Color(0xFF0E1512),
    surface = Color(0xFF16201B),
    surfaceSoft = Color(0xFF1E2A23),
    surfaceElevated = Color(0xFF243029),
    textPrimary = Color(0xFFEFE8D8),
    textSecondary = Color(0xFFB8AC96),
    textHint = Color(0xFF7A7262),
    accent = Color(0xFFB89B5E),
    accentSoft = Color(0xFF6F5C36),
    accentDim = Color(0xFF3D3220)
)

val WitchLibraryPalette = TalesbookPalette(
    background = Color(0xFF100D18),
    surface = Color(0xFF1A1625),
    surfaceSoft = Color(0xFF221D2E),
    surfaceElevated = Color(0xFF2A2438),
    textPrimary = Color(0xFFE8E0F0),
    textSecondary = Color(0xFF9A8CAA),
    textHint = Color(0xFF5A4A6A),
    accent = Color(0xFF9B7FC7),
    accentSoft = Color(0xFF5A4478),
    accentDim = Color(0xFF2E2240)
)

val MoonGardenPalette = TalesbookPalette(
    background = Color(0xFF0D1520),
    surface = Color(0xFF141E2E),
    surfaceSoft = Color(0xFF1B2638),
    surfaceElevated = Color(0xFF212E42),
    textPrimary = Color(0xFFDDE6F0),
    textSecondary = Color(0xFF8A9AB0),
    textHint = Color(0xFF4A5A6A),
    accent = Color(0xFF8BA4C7),
    accentSoft = Color(0xFF4A6080),
    accentDim = Color(0xFF263040)
)

val HobbitRoomPalette = TalesbookPalette(
    background = Color(0xFF1A1208),
    surface = Color(0xFF241A0E),
    surfaceSoft = Color(0xFF2E2214),
    surfaceElevated = Color(0xFF382A1A),
    textPrimary = Color(0xFFF0E8D0),
    textSecondary = Color(0xFFB0966A),
    textHint = Color(0xFF6A5030),
    accent = Color(0xFFC8963C),
    accentSoft = Color(0xFF7A5822),
    accentDim = Color(0xFF3E2C10)
)

val LocalTalesbookColors = staticCompositionLocalOf { EveningForestPalette }
