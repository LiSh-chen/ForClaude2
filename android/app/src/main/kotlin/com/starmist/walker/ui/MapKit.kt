package com.starmist.walker.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.layout.onSizeChanged
import kotlin.math.min

/**
 * A map the player can pinch and drag. It opens zoomed in on [focus] (map coordinates, 0 to 1) so
 * that progress is visible, follows [focus] as it moves unless the player has been moving the map
 * lately, and cannot be zoomed out further than showing the whole map.
 *
 * [content] draws the map in a coordinate space of exactly [w] by [h] pixels, already shifted to the
 * right place; [zoom] is how far in it is, for details that appear only when close.
 */
@Composable
fun ZoomPanMap(
    aspect: Float,
    focus: Offset,
    followKey: Any?,
    modifier: Modifier = Modifier,
    initialZoom: Float = 3f,
    maxZoom: Float = 8f,
    background: Color = Vintage.parchmentLight,
    overlay: @Composable BoxScope.() -> Unit = {},
    content: DrawScope.(w: Float, h: Float, zoom: Float, view: Rect) -> Unit,
) {
    var zoom by remember(aspect) { mutableFloatStateOf(initialZoom) }
    var center by remember(aspect) { mutableStateOf(focus) }
    var lastTouch by remember { mutableLongStateOf(0L) }
    var viewport by remember { mutableStateOf(IntSize.Zero) }

    fun clamp(c: Offset, z: Float): Offset {
        if (viewport.width == 0 || viewport.height == 0) return c
        val vw = viewport.width.toFloat()
        val vh = viewport.height.toFloat()
        val w0 = min(vw, vh * aspect)
        val h0 = w0 / aspect
        val halfX = vw / 2f / (w0 * z)
        val halfY = vh / 2f / (h0 * z)
        val x = if (halfX >= 0.5f) 0.5f else c.x.coerceIn(halfX, 1f - halfX)
        val y = if (halfY >= 0.5f) 0.5f else c.y.coerceIn(halfY, 1f - halfY)
        return Offset(x, y)
    }

    // Follow the walker, unless the player is looking around.
    LaunchedEffect(followKey, viewport) {
        if (System.currentTimeMillis() - lastTouch > FOLLOW_PAUSE_MS) center = clamp(focus, zoom)
    }

    Box(
        modifier
            .clipToBounds()
            .background(background)
            .onSizeChanged { viewport = it }
            .pointerInput(aspect) {
                detectTransformGestures { _, pan, gestureZoom, _ ->
                    lastTouch = System.currentTimeMillis()
                    val newZoom = (zoom * gestureZoom).coerceIn(1f, maxZoom)
                    val vw = size.width.toFloat()
                    val vh = size.height.toFloat()
                    val w0 = min(vw, vh * aspect)
                    val h0 = w0 / aspect
                    val moved = Offset(center.x - pan.x / (w0 * newZoom), center.y - pan.y / (h0 * newZoom))
                    zoom = newZoom
                    center = clamp(moved, newZoom)
                }
            },
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val w0 = min(size.width, size.height * aspect)
            val h0 = w0 / aspect
            val w = w0 * zoom
            val h = h0 * zoom
            val c = clamp(center, zoom)
            val view = Rect(c.x * w - size.width / 2f, c.y * h - size.height / 2f, c.x * w + size.width / 2f, c.y * h + size.height / 2f)
            translate(size.width / 2f - c.x * w, size.height / 2f - c.y * h) { content(w, h, zoom, view) }
        }
        overlay()
        Box(
            Modifier
                .align(Alignment.BottomEnd)
                .padding(8.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Vintage.parchmentLight.copy(alpha = 0.9f))
                .clickable {
                    lastTouch = 0L
                    zoom = initialZoom
                    center = clamp(focus, initialZoom)
                }
                .padding(horizontal = 12.dp, vertical = 6.dp),
        ) { Text("回到我的位置", style = MaterialTheme.typography.labelMedium) }
    }
}

private const val FOLLOW_PAUSE_MS = 8_000L
