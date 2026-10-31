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
 * so the picture stays sharp (it is drawn at its real size, not stretched). The text follows that scale and the size the user picked in the settings (see [AppFontScale]), not the system's font-size setting.
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
            CompositionLocalProvider(LocalDensity provides Density(outer.density * fit, fontScale = outer.fontScale)) {
                Box(Modifier.fillMaxSize()) { content() }
            }
        }
    }
}

/** How much bigger or smaller the app's writing is than the design for each of the three sizes the user can pick (small, medium, large). */
internal fun fontScaleOf(step: Int): Float = when (step) { 0 -> 0.85f; 2 -> 1.25f; else -> 1f }

/** The app's writing at the size the user picked, whatever the system's font-size setting is: the layouts are made and checked for these three sizes only. */
@Composable
internal fun AppFontScale(step: Int, content: @Composable () -> Unit) {
    val d = LocalDensity.current
    CompositionLocalProvider(LocalDensity provides Density(d.density, fontScale = fontScaleOf(step))) { content() }
}

/**
 * [content] with the app's font size followed only half way: the writing on the dials sits on a drawn scale, and at the largest size it would run into the numbers
 * round it, so the dials get a smaller step between small, medium and large than the text of the rest of the app.
 */
@Composable
internal fun HalfFontScale(content: @Composable () -> Unit) {
    val d = LocalDensity.current
    CompositionLocalProvider(LocalDensity provides Density(d.density, fontScale = 1f + (d.fontScale - 1f) * 0.5f)) { content() }
}
