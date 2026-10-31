package com.starmist.walker.ui

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp

/**
 * Lays [content] out on a fixed design size ([designWidth] x [designHeight]) and scales the whole of it, writing included, to the room there is, so that it looks
 * the same on a narrow phone and on a wide one: nothing in it can be cut off by the width of the screen. It is done by giving the content a density scaled to fit,
 * so the picture stays sharp (it is drawn at its real size, not stretched). The text follows that scale and not the system's font-size setting, which would
 * otherwise make the writing wider than the room it was laid out for.
 */
@Composable
internal fun FitDesign(designWidth: Dp, designHeight: Dp, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    BoxWithConstraints(modifier, contentAlignment = Alignment.Center) {
        val outer = LocalDensity.current
        val fit = minOf(
            constraints.maxWidth / (designWidth.value * outer.density),
            constraints.maxHeight / (designHeight.value * outer.density),
        ).coerceIn(0.6f, 1.6f)
        Box(Modifier.size(designWidth * fit, designHeight * fit)) {
            CompositionLocalProvider(LocalDensity provides Density(outer.density * fit, fontScale = 1f)) {
                Box(Modifier.fillMaxSize()) { content() }
            }
        }
    }
}

/** [content] with the writing at its own size whatever the system's font-size setting: for the dials, where the writing sits on a drawn scale. */
@Composable
internal fun OwnFontSize(content: @Composable () -> Unit) {
    val d = LocalDensity.current
    CompositionLocalProvider(LocalDensity provides Density(d.density, fontScale = 1f)) { content() }
}
