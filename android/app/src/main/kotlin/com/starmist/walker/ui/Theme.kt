package com.starmist.walker.ui

import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.ImageShader
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlin.math.hypot
import kotlin.random.Random

/** The look of the app: an old botanist's field notebook. Warm paper, brown ink, moss and brass. */
object Vintage {
    val parchment = Color(0xFFF1E3BE)
    val parchmentLight = Color(0xFFF7EBCB)
    val parchmentDark = Color(0xFFE6D3A5)
    val edge = Color(0xFFB98F52)
    val stain = Color(0xFFC9A263)
    val ink = Color(0xFF3B2A1A)
    val inkSoft = Color(0xFF6A5236)
    val moss = Color(0xFF5F8A52)
    val mossLight = Color(0xFFB4CDA4)
    val brass = Color(0xFFB8892D)
    val brassLight = Color(0xFFE6C874)
    val brassDark = Color(0xFF7C5A1B)
    val rose = Color(0xFFD3A6BC)
    val sea = Color(0xFF8DB4C4)
    val fog = Color(0xFFD9D0B9)
    val night = Color(0xFF1B2A55)
}

private val colors = lightColorScheme(
    primary = Color(0xFF6B4A24),
    onPrimary = Vintage.parchmentLight,
    primaryContainer = Vintage.brassLight,
    onPrimaryContainer = Vintage.ink,
    secondary = Vintage.moss,
    onSecondary = Vintage.parchmentLight,
    secondaryContainer = Color(0xFFD9DDB5),
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
    error = Color(0xFF9B3B2B),
    errorContainer = Color(0xFFE8C4B0),
    onErrorContainer = Vintage.ink,
)

private fun serif(style: TextStyle) = style.copy(fontFamily = FontFamily.Serif)

private val typography = Typography().let {
    it.copy(
        displayLarge = serif(it.displayLarge), displayMedium = serif(it.displayMedium), displaySmall = serif(it.displaySmall),
        headlineLarge = serif(it.headlineLarge), headlineMedium = serif(it.headlineMedium), headlineSmall = serif(it.headlineSmall),
        titleLarge = serif(it.titleLarge), titleMedium = serif(it.titleMedium), titleSmall = serif(it.titleSmall),
        bodyLarge = serif(it.bodyLarge), bodyMedium = serif(it.bodyMedium), bodySmall = serif(it.bodySmall),
        labelLarge = serif(it.labelLarge), labelMedium = serif(it.labelMedium), labelSmall = serif(it.labelSmall),
    )
}

/**
 * A rectangle whose edges are slightly ragged, like a torn scrap of paper. Same seed, same tear.
 * Material's [Shapes] only accepts corner-based shapes, so the "corner size" is used as the depth of
 * the tear.
 */
class TornShape(private val seed: Int, jag: CornerSize) : CornerBasedShape(jag, jag, jag, jag) {
    constructor(seed: Int, jag: Dp) : this(seed, CornerSize(jag))

    override fun createOutline(
        size: Size,
        topStart: Float,
        topEnd: Float,
        bottomEnd: Float,
        bottomStart: Float,
        layoutDirection: LayoutDirection,
    ): Outline {
        val amplitude = topStart
        val spacing = (amplitude * 4.5f).coerceAtLeast(24f)
        val random = Random(seed)
        val path = Path()
        val corners = listOf(
            Offset(0f, 0f), Offset(size.width, 0f), Offset(size.width, size.height), Offset(0f, size.height),
        )
        path.moveTo(corners[0].x, corners[0].y)
        for (i in corners.indices) {
            val from = corners[i]
            val to = corners[(i + 1) % corners.size]
            val dx = to.x - from.x
            val dy = to.y - from.y
            val length = hypot(dx, dy)
            if (length > 0f) {
                val parts = (length / spacing).toInt().coerceAtLeast(1)
                // Clockwise edges: the inward normal is the direction rotated a quarter turn.
                val nx = -dy / length
                val ny = dx / length
                for (k in 1 until parts) {
                    val t = k.toFloat() / parts
                    val bite = random.nextFloat() * amplitude
                    path.lineTo(from.x + dx * t + nx * bite, from.y + dy * t + ny * bite)
                }
            }
            path.lineTo(to.x, to.y)
        }
        path.close()
        return Outline.Generic(path)
    }

    override fun copy(topStart: CornerSize, topEnd: CornerSize, bottomEnd: CornerSize, bottomStart: CornerSize): CornerBasedShape =
        TornShape(seed, topStart)
}

private val shapes = Shapes(
    extraSmall = TornShape(1, 1.dp),
    small = TornShape(2, 1.5.dp),
    medium = TornShape(3, 3.dp),
    large = TornShape(4, 4.dp),
)

/** Old paper under everything: warm base, stains, speckles and darker edges. Drawn once per size. */
fun Modifier.parchmentTexture(tile: ImageBitmap? = null): Modifier = drawWithCache {
    val random = Random(7)
    val width = size.width
    val height = size.height
    val specks = List(260) {
        Triple(Offset(random.nextFloat() * width, random.nextFloat() * height), 0.8f + random.nextFloat() * 2.2f, 0.03f + random.nextFloat() * 0.07f)
    }
    val stains = List(6) {
        Triple(Offset(random.nextFloat() * width, random.nextFloat() * height), size.minDimension * (0.12f + random.nextFloat() * 0.2f), 0.05f + random.nextFloat() * 0.06f)
    }
    val vignette = Brush.radialGradient(
        colors = listOf(Color.Transparent, Vintage.edge.copy(alpha = 0.38f)),
        center = Offset(width / 2, height / 2),
        radius = hypot(width, height) / 2,
    )
    // A painted paper tile from the asset library, if there is one; otherwise draw the texture in code.
    onDrawBehind {
        if (tile != null) {
            // A painted full-page paper: scale to cover the screen and crop the overflow.
            val scale = maxOf(width / tile.width, height / tile.height)
            val srcW = (width / scale).toInt().coerceAtMost(tile.width)
            val srcH = (height / scale).toInt().coerceAtMost(tile.height)
            drawImage(
                tile,
                srcOffset = androidx.compose.ui.unit.IntOffset((tile.width - srcW) / 2, (tile.height - srcH) / 2),
                srcSize = androidx.compose.ui.unit.IntSize(srcW, srcH),
                dstSize = androidx.compose.ui.unit.IntSize(width.toInt(), height.toInt()),
            )
        } else {
            drawRect(Vintage.parchment)
            stains.forEach { (center, radius, alpha) -> drawCircle(Vintage.stain.copy(alpha = alpha), radius, center) }
            specks.forEach { (center, radius, alpha) -> drawCircle(Vintage.ink.copy(alpha = alpha), radius, center) }
        }
        drawRect(vignette)
    }
}

@Composable
fun StarMistTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = colors, typography = typography, shapes = shapes, content = content)
}
