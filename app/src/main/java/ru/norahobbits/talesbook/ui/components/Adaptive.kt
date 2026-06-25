package ru.norahobbits.talesbook.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

enum class WindowSizeClass {
    Compact,
    Medium,
    Expanded
}

@Composable
fun rememberWindowSizeClass(): WindowSizeClass {
    val width = LocalConfiguration.current.screenWidthDp
    return when {
        width >= 840 -> WindowSizeClass.Expanded
        width >= 600 -> WindowSizeClass.Medium
        else -> WindowSizeClass.Compact
    }
}

fun WindowSizeClass.contentMaxWidth(): Dp = when (this) {
    WindowSizeClass.Compact -> Dp.Unspecified
    WindowSizeClass.Medium -> 680.dp
    WindowSizeClass.Expanded -> 920.dp
}
