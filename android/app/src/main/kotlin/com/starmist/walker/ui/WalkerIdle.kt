package com.starmist.walker.ui

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

/**
 * Little things the traveller does while it stands still, one every few seconds: a stretch, a look round, humming and hopping to
 * its own tune, and a doze that ends with a start. Each runs for [MS] milliseconds; [t] below runs 0 to 1 over that time.
 * They only squash, tip and hop the standing picture and float a mark above the head, so they need no new drawings.
 */
internal object Idle {
    const val NONE = 0
    const val STRETCH = 1
    const val LOOK = 2
    const val HUM = 3
    const val NAP = 4
    const val COUNT = 4
    val MS = intArrayOf(0, 2600, 2800, 3200, 4200)
}

/** How the standing picture is moved: scaled about the feet, tipped, lifted by a fraction of its height, and turned (-1 is mirrored). */
internal class IdlePose(val sx: Float = 1f, val sy: Float = 1f, val rot: Float = 0f, val lift: Float = 0f, val face: Float = 1f)

private fun smooth(a: Float, b: Float, x: Float): Float {
    val u = ((x - a) / (b - a)).coerceIn(0f, 1f)
    return u * u * (3f - 2f * u)
}

private fun bell(u: Float): Float = if (u <= 0f || u >= 1f) 0f else sin(PI.toFloat() * u)

internal fun idlePose(act: Int, t: Float): IdlePose = when (act) {
    Idle.STRETCH -> {
        val up = smooth(0f, 0.3f, t) * (1f - smooth(0.55f, 0.65f, t))                  // reaching up
        val out = smooth(0.6f, 0.72f, t) * (1f - smooth(0.82f, 1f, t))                 // letting the breath out
        IdlePose(sx = 1f - 0.05f * up + 0.06f * out, sy = 1f + 0.10f * up - 0.07f * out, rot = 2.5f * sin(t * 6f * PI.toFloat()) * up)
    }
    Idle.LOOK -> {
        val a = smooth(0.12f, 0.28f, t); val b = smooth(0.52f, 0.68f, t)
        val f = a - b                                                                   // 0 facing front, 1 turned right round
        IdlePose(face = cos(PI.toFloat() * f), lift = 0.03f * bell((t - 0.12f) / 0.16f) + 0.03f * bell((t - 0.52f) / 0.16f))
    }
    Idle.HUM -> {
        val env = smooth(0f, 0.08f, t) * (1f - smooth(0.92f, 1f, t))
        val hop = abs(sin(PI.toFloat() * 3f * t))                                       // three hops to the beat
        val squash = env * (0.05f * hop - 0.06f * (1f - hop))                          // stretched in the air, squashed on landing; none at the start and end
        IdlePose(sx = 1f - squash * 0.6f, sy = 1f + squash, rot = 4f * sin(t * 3f * PI.toFloat()) * env, lift = 0.07f * hop * env)
    }
    Idle.NAP -> {
        val yawn = sin(PI.toFloat() * (t / 0.15f).coerceIn(0f, 1f))
        val droop = smooth(0.12f, 0.3f, t) * (1f - smooth(0.78f, 0.82f, t))
        val start = ((t - 0.8f) / 0.14f).coerceIn(0f, 1f)
        IdlePose(
            sx = 1f + 0.04f * droop - 0.02f * yawn,
            sy = 1f + 0.05f * yawn - 0.07f * droop + 0.06f * sin(PI.toFloat() * start),
            rot = droop * (6f + 3f * sin((t - 0.2f) * 2f * PI.toFloat() * 1.1f)),
            lift = 0.10f * sin(PI.toFloat() * start),
        )
    }
    else -> IdlePose()
}

/** The marks that float up above the head: a sparkle, a question mark, music notes, sleepy Z's and a startled "!". [head] is just above the head, [size] the figure's height, [sign] the way it faces. */
internal fun DrawScope.drawIdleEmote(m: TextMeasurer, act: Int, t: Float, head: Offset, size: Float, sign: Float) {
    fun mark(text: String, x: Float, y: Float, scale: Float, color: Color, alpha: Float) {
        if (alpha <= 0.01f) return
        val style = TextStyle(fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold, fontSize = (size * 0.30f * scale).toSp(), color = color.copy(alpha = alpha))
        val halo = m.measure(text, style.copy(color = Color.White.copy(alpha = 0.85f * alpha)))
        val tl = Offset(x - halo.size.width / 2f, y - halo.size.height / 2f)
        for (dx in floatArrayOf(-1.4f, 1.4f)) for (dy in floatArrayOf(-1.4f, 1.4f)) drawText(halo, topLeft = Offset(tl.x + dx, tl.y + dy))
        drawText(m.measure(text, style), topLeft = tl)
    }
    when (act) {
        Idle.STRETCH -> {
            mark("✦", head.x - size * 0.34f, head.y + size * 0.10f - size * 0.06f * (t - 0.6f), 0.7f, Vintage.brass, bell((t - 0.6f) / 0.35f))
            mark("✦", head.x + size * 0.32f, head.y + size * 0.02f - size * 0.06f * (t - 0.65f), 0.55f, Vintage.brass, bell((t - 0.65f) / 0.3f))
        }
        Idle.LOOK -> mark("？", head.x + sign * size * 0.18f, head.y - size * 0.05f - size * 0.08f * (t - 0.3f), 1f, Vintage.brassDark, bell((t - 0.3f) / 0.26f))
        Idle.HUM -> for (i in 0..2) {
            val u = (t - (0.08f + 0.27f * i)) / 0.4f
            mark(if (i == 1) "♫" else "♪", head.x + sign * size * (0.26f + 0.1f * (i % 2)) + size * 0.04f * sin(u * 6f), head.y + size * 0.05f - size * 0.40f * u, 0.9f, Vintage.brassDark, bell(u))
        }
        Idle.NAP -> {
            for (i in 0..2) {
                val u = (t - (0.18f + 0.18f * i)) / 0.42f
                mark("Z", head.x + sign * size * (0.20f + 0.12f * i), head.y + size * 0.05f - size * 0.30f * u, 0.55f + 0.2f * i, Color(0xFF5A7FA8), bell(u))
            }
            val s = (t - 0.8f) / 0.17f
            mark("！", head.x, head.y - size * 0.06f, 1.1f + 0.3f * sin(PI.toFloat() * s.coerceIn(0f, 1f)), Color(0xFFB3202A), bell(s))
        }
    }
}
