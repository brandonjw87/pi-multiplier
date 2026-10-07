package com.brandonwallace.pimultiplier

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val Gold = Color(0xFFFFC94A)
val GoldSoft = Color(0xFFFFE6A3)
val Lavender = Color(0xFFCDBBFF)
val DeepPurple = Color(0xFF4A2C8A)
val Night = Color(0xFF120A24)
val NightMid = Color(0xFF1C0F3D)
val CardBg = Color(0xD9201440)

private val PiColors = darkColorScheme(
    primary = Gold,
    onPrimary = Color(0xFF2A1A00),
    primaryContainer = DeepPurple,
    onPrimaryContainer = GoldSoft,
    secondary = Lavender,
    onSecondary = Color(0xFF1E0F45),
    secondaryContainer = Color(0xFF3A2468),
    onSecondaryContainer = Color(0xFFEDE3FF),
    tertiary = Color(0xFFFF9EC4),
    background = Night,
    onBackground = Color(0xFFEDE6FF),
    surface = Color(0xFF1A1033),
    onSurface = Color(0xFFEDE6FF),
    surfaceVariant = Color(0xFF2A1D4D),
    onSurfaceVariant = Color(0xFFC9BCE8),
    outline = Color(0xFF7D6BA8),
    outlineVariant = Color(0xFF433366),
    surfaceContainerLowest = Night,
    surfaceContainerLow = Color(0xFF1A1033),
    surfaceContainer = Color(0xFF21153F),
    surfaceContainerHigh = Color(0xFF2A1D4D),
    surfaceContainerHighest = Color(0xFF34265C),
)

@Composable
fun PiTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = PiColors, typography = Typography(), content = content)
}
