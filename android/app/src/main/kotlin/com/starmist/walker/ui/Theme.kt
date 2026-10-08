package com.starmist.walker.ui

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp

/** The look of the app: an old botanist's field notebook. Warm paper, brown ink, moss and brass. */
object Vintage {
    val parchment = Color(0xFFF3E7CF)
    val parchmentLight = Color(0xFFFBF4E3)
    val parchmentDark = Color(0xFFF8EEDA)
    val edge = Color(0xFFD8C7A0)
    val stain = Color(0xFFE8D5B0)
    val ink = Color(0xFF2E3E3B)
    val inkSoft = Color(0xFF6B5A48)
    val moss = Color(0xFF3F7A66)
    val mossLight = Color(0xFF9CC3AE)
    val brass = Color(0xFFE8B44F)
    val brassLight = Color(0xFFF6D9A8)
    val brassDark = Color(0xFFC9923A)
    val rose = Color(0xFFE58A57)
    val sea = Color(0xFF6E9CA0)
    val fog = Color(0xFFE4DCC8)
    val night = Color(0xFF1F3A44)
    val teal = Color(0xFF2F5D62)
    val tealDeep = Color(0xFF2A4B4F)
    val orange = Color(0xFFD9744B)
    val hillFar = Color(0xFF6E9C86)
    val hillMid = Color(0xFF3F7A66)
    val hillNear = Color(0xFF2F5D4E)
}

private val colors = lightColorScheme(
    primary = Vintage.teal,
    onPrimary = Vintage.parchmentLight,
    primaryContainer = Vintage.brassLight,
    onPrimaryContainer = Vintage.ink,
    secondary = Vintage.moss,
    onSecondary = Vintage.parchmentLight,
    secondaryContainer = Color(0xFFDDE8D2),
    onSecondaryContainer = Vintage.ink,
    tertiary = Vintage.moss,
    background = Vintage.parchment,
    onBackground = Vintage.ink,
    surface = Vintage.parchment,
    onSurface = Vintage.ink,
    surfaceVariant = Vintage.parchmentDark,
    onSurfaceVariant = Vintage.ink,
    surfaceContainer = Vintage.parchmentDark,
    surfaceContainerHighest = Vintage.parchmentDark,
    outline = Vintage.inkSoft,
    outlineVariant = Vintage.edge,
    error = Vintage.orange,
    errorContainer = Color(0xFFF6D3BE),
    onErrorContainer = Vintage.ink,
)

private fun serif(style: TextStyle) = style.copy(fontFamily = FontFamily.SansSerif)

private val typography = Typography().let {
    it.copy(
        displayLarge = serif(it.displayLarge), displayMedium = serif(it.displayMedium), displaySmall = serif(it.displaySmall),
        headlineLarge = serif(it.headlineLarge), headlineMedium = serif(it.headlineMedium), headlineSmall = serif(it.headlineSmall),
        titleLarge = serif(it.titleLarge), titleMedium = serif(it.titleMedium), titleSmall = serif(it.titleSmall),
        bodyLarge = serif(it.bodyLarge), bodyMedium = serif(it.bodyMedium), bodySmall = serif(it.bodySmall),
        labelLarge = serif(it.labelLarge), labelMedium = serif(it.labelMedium), labelSmall = serif(it.labelSmall),
    )
}

private val shapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(18.dp),
)

/** Flat paper-cut backdrop: warm paper with a soft darker corner. [tile] is unused and kept for callers. */
@Suppress("UNUSED_PARAMETER")
fun Modifier.parchmentTexture(tile: ImageBitmap? = null): Modifier = drawWithCache {
    val wash = Brush.verticalGradient(listOf(Vintage.parchmentLight, Vintage.parchment), 0f, size.height)
    onDrawBehind { drawRect(wash) }
}

@Composable
fun StarMistTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = colors, typography = typography, shapes = shapes, content = content)
}
