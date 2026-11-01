package com.prootagent.app.ui.theme
import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat

private val DarkColors = darkColorScheme(
    primary = AccentBlue, secondary = AccentTeal, background = LiquidBgStart,
    surface = Color.Transparent, onBackground = TextPrimary, onSurface = TextPrimary
)

fun Modifier.liquidGlass() = this
    .background(color = LiquidGlassBg, shape = RoundedCornerShape(24.dp))
    .border(width = 1.dp, brush = Brush.linearGradient(listOf(LiquidBorder, Color.Transparent)), shape = RoundedCornerShape(24.dp))

@Composable
fun ProotAgentTheme(content: @Composable () -> Unit) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect { WindowCompat.getInsetsController((view.context as Activity).window, view).isAppearanceLightStatusBars = false }
    }
    MaterialTheme(colorScheme = DarkColors, content = content)
}
