package com.starmist.walker.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

private val SKIN = Color(0xFFE8C39E)
private val DARK = Color(0xFF3B2A1A)

/** A small round portrait of a traveler, drawn from a few shapes in the notebook's colours. */
@Composable
internal fun TravelerAvatar(id: String, size: Dp, modifier: Modifier = Modifier, dim: Boolean = false) {
    // the painted portrait (res/drawable-nodpi/traveler_<id>.webp) when there is one; the shapes below are the fallback
    val context = LocalContext.current
    val resId = remember(id) { context.resources.getIdentifier("traveler_$id", "drawable", context.packageName) }
    if (resId != 0) { PortraitAvatar(resId, size, modifier, dim); return }
    Canvas(modifier.size(size)) {
        val w = this.size.minDimension
        val shirt = when (id) {
            "ahbo" -> Vintage.moss; "xiaohe" -> Color(0xFFF1E3BE); "xipo" -> Vintage.sea; "atie" -> Color(0xFF2F4A6B); "momo" -> Vintage.rose
            else -> Vintage.fog
        }
        drawCircle(Vintage.parchmentDark, radius = w / 2f)
        // shoulders
        drawRoundRect(shirt, Offset(w * 0.18f, w * 0.68f), Size(w * 0.64f, w * 0.34f), CornerRadius(w * 0.2f))
        // head
        val c = Offset(w * 0.5f, w * 0.44f)
        drawCircle(SKIN, radius = w * 0.2f, center = c)
        drawCircle(DARK, radius = w * 0.022f, center = Offset(c.x - w * 0.07f, c.y + w * 0.02f))
        drawCircle(DARK, radius = w * 0.022f, center = Offset(c.x + w * 0.07f, c.y + w * 0.02f))
        drawArc(DARK, 20f, 140f, false, Offset(c.x - w * 0.06f, c.y + w * 0.03f), Size(w * 0.12f, w * 0.08f), style = androidx.compose.ui.graphics.drawscope.Stroke(w * 0.012f))
        accessory(id, w, c)
        if (dim) drawCircle(Vintage.parchment.copy(alpha = 0.72f), radius = w / 2f)
    }
}

private fun DrawScope.accessory(id: String, w: Float, c: Offset) {
    when (id) {
        "ahbo" -> { // green cap and a camera
            drawArc(Color(0xFF3F6B45), 180f, 180f, true, Offset(c.x - w * 0.21f, c.y - w * 0.22f), Size(w * 0.42f, w * 0.34f))
            drawRoundRect(Color(0xFF3F6B45), Offset(c.x - w * 0.02f, c.y - w * 0.07f), Size(w * 0.27f, w * 0.05f), CornerRadius(w * 0.02f))
            drawRoundRect(DARK, Offset(w * 0.36f, w * 0.76f), Size(w * 0.28f, w * 0.15f), CornerRadius(w * 0.03f))
            drawCircle(Color(0xFF9AA7AD), radius = w * 0.045f, center = Offset(w * 0.5f, w * 0.835f))
        }
        "xiaohe" -> { // straw hat
            drawOval(Vintage.brassLight, Offset(c.x - w * 0.34f, c.y - w * 0.13f), Size(w * 0.68f, w * 0.14f))
            drawArc(Vintage.brass, 180f, 180f, true, Offset(c.x - w * 0.17f, c.y - w * 0.3f), Size(w * 0.34f, w * 0.34f))
            drawRect(Color(0xFFB4533C), Offset(c.x - w * 0.17f, c.y - w * 0.14f), Size(w * 0.34f, w * 0.03f))
        }
        "xipo" -> { // blue headscarf
            drawArc(Color(0xFF3E6E8E), 180f, 180f, true, Offset(c.x - w * 0.22f, c.y - w * 0.23f), Size(w * 0.44f, w * 0.4f))
            drawCircle(Color(0xFF3E6E8E), radius = w * 0.045f, center = Offset(c.x + w * 0.2f, c.y - w * 0.02f))
        }
        "atie" -> { // railway cap and a lunch box
            drawArc(Color(0xFF1F2F46), 180f, 180f, true, Offset(c.x - w * 0.21f, c.y - w * 0.22f), Size(w * 0.42f, w * 0.34f))
            drawRect(Color(0xFF1F2F46), Offset(c.x - w * 0.21f, c.y - w * 0.1f), Size(w * 0.42f, w * 0.04f))
            drawCircle(Vintage.brassLight, radius = w * 0.03f, center = Offset(c.x, c.y - w * 0.14f))
            drawRoundRect(Color(0xFFB9C0C4), Offset(w * 0.38f, w * 0.78f), Size(w * 0.24f, w * 0.13f), CornerRadius(w * 0.02f))
        }
        "momo" -> { // red beret and a sketchbook
            drawOval(Color(0xFFB3202A), Offset(c.x - w * 0.24f, c.y - w * 0.27f), Size(w * 0.5f, w * 0.2f))
            drawCircle(Color(0xFFB3202A), radius = w * 0.02f, center = Offset(c.x + w * 0.02f, c.y - w * 0.29f))
            drawRoundRect(Color(0xFF7C5A1B), Offset(w * 0.55f, w * 0.74f), Size(w * 0.2f, w * 0.17f), CornerRadius(w * 0.02f))
        }
    }
}

/** The painted portrait cropped to a circle on the notebook's paper: the sheet's own paper colour is multiplied away. */
@Composable
private fun PortraitAvatar(resId: Int, size: Dp, modifier: Modifier, dim: Boolean) {
    val bmp: ImageBitmap = ImageBitmap.imageResource(resId)
    Canvas(modifier.size(size)) {
        val w = this.size.minDimension
        val circle = Path().apply { addOval(Rect(0f, 0f, w, w)) }
        clipPath(circle) {
            drawRect(Vintage.parchment)
            val side = (bmp.width * 0.84f).toInt()
            drawImage(
                bmp, srcOffset = IntOffset((bmp.width * 0.08f).toInt(), (bmp.width * 0.04f).toInt()), srcSize = IntSize(side, side),
                dstSize = IntSize(w.toInt(), w.toInt()), blendMode = BlendMode.Multiply,
            )
            if (dim) drawRect(Vintage.parchment.copy(alpha = 0.72f))
        }
        drawCircle(Vintage.edge, radius = w / 2f - 0.75.dp.toPx(), style = Stroke(1.5.dp.toPx()))
    }
}
