package com.prootagent.app.ui.theme
import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat

data class GlassColors(
    val bgStart: Color, val bgEnd: Color, val glassBg: Color, val border: Color,
    val textPrimary: Color, val textSecondary: Color
)

val LocalGlassColors = staticCompositionLocalOf {
    GlassColors(DarkBgStart, DarkBgEnd, DarkGlassBg, DarkBorder, DarkText, DarkSubText)
}

@Composable
fun Modifier.liquidGlass() = this
    .background(LocalGlassColors.current.glassBg, RoundedCornerShape(20.dp))
    .border(1.dp, Brush.linearGradient(listOf(LocalGlassColors.current.border, Color.Transparent)), RoundedCornerShape(20.dp))

@Composable
fun ProotAgentTheme(themeMode: Int = 0, content: @Composable () -> Unit) {
    val darkTheme = when (themeMode) {
        1 -> true
        2 -> false
        else -> isSystemInDarkTheme()
    }
    
    val glassColors = if (darkTheme) GlassColors(DarkBgStart, DarkBgEnd, DarkGlassBg, DarkBorder, DarkText, DarkSubText)
                      else GlassColors(LightBgStart, LightBgEnd, LightGlassBg, LightBorder, LightText, LightSubText)
                      
    val colorScheme = if (darkTheme) darkColorScheme(primary = AccentBlue, background = DarkBgStart, surface = Color.Transparent, onBackground = DarkText)
                      else lightColorScheme(primary = AccentBlue, background = LightBgStart, surface = Color.Transparent, onBackground = LightText)

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect { WindowCompat.getInsetsController((view.context as Activity).window, view).isAppearanceLightStatusBars = !darkTheme }
    }

    CompositionLocalProvider(LocalGlassColors provides glassColors) {
        MaterialTheme(colorScheme = colorScheme, content = content)
    }
}
