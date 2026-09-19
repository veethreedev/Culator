package com.example.culator.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance

@Composable
fun CulatorTheme(darkTheme: Boolean = true, accent: String = "Mint", customAccent: Int = 0xFFCAEFAC.toInt(), content: @Composable () -> Unit) {
    val colors = when (accent) {
        "Blue" -> 0xFFADCFFF to 0xFF245DA8
        "Purple" -> 0xFFD6BCFF to 0xFF7041AC
        else -> 0xFFCAEFAC to 0xFF416B24
    }
    val primary = if (accent == "Custom") Color(customAccent) else Color(if (darkTheme) colors.first else colors.second)
    val onPrimary = if (primary.luminance() > 0.179f) Color.Black else Color.White
    val background = Color(if (darkTheme) 0xFF101214 else 0xFFF6F7F4)
    val surface = Color(if (darkTheme) 0xFF202427 else 0xFFE7EAE4)
    val foreground = Color(if (darkTheme) 0xFFF2F4EF else 0xFF191D17)
    val container = primary.copy(alpha = 0.16f)
    val scheme = if (darkTheme) darkColorScheme(
        primary = primary, onPrimary = onPrimary, background = background, onBackground = foreground,
        surface = surface, onSurface = foreground, onSurfaceVariant = Color(0xFFABB3AD),
        primaryContainer = container, onPrimaryContainer = primary,
        secondaryContainer = container, onSecondaryContainer = primary
    ) else lightColorScheme(
        primary = primary, onPrimary = onPrimary, background = background, onBackground = foreground,
        surface = surface, onSurface = foreground, onSurfaceVariant = Color(0xFF555E52),
        primaryContainer = container, onPrimaryContainer = primary,
        secondaryContainer = container, onSecondaryContainer = primary
    )
    MaterialTheme(colorScheme = scheme, typography = Typography, content = content)
}
