package com.starmist.walker.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.starmist.walker.data.Ledger
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min

/** A line about a township that does not give its name away: where it is and how big and how high. */
@Composable
internal fun postcardHint(t: TownArea): String {
    val kind = when {
        t.highElevM >= 2500 -> tr("高山")
        t.highElevM >= 1000 -> tr("山地")
        t.medianElevM <= 30 -> tr("近海平原")
        else -> tr("丘陵台地")
    }
    return trf("{0}・{1}・約 {2} 平方公里", t.county, kind, "%.0f".format(t.areaKm2))
}

/** What a postcard says, from the township's own figures (more words can be added per township later). */
@Composable
internal fun postcardBody(t: TownArea): List<String> = listOf(
    trf("面積約 {0} 平方公里，共 {1} 個村里。", "%.1f".format(t.areaKm2), t.villages),
    trf("境內最高處海拔約 {0} 公尺，中位數約 {1} 公尺。", t.highElevM, t.medianElevM),
)

/** The outline of a township, drawn to fit its box (north up). */
@Composable
internal fun TownShape(t: TownArea, fill: Color, line: Color, modifier: Modifier = Modifier) {
    val path = remember(t) { outlineOf(t) }
    Canvas(modifier) {
        val b = path.bounds
        val s = min(size.width / b.w, size.height / b.h) * 0.92f
        val ox = (size.width - b.w * s) / 2f
        val oy = (size.height - b.h * s) / 2f
        for (ring in path.rings) {
            val p = Path()
            ring.forEachIndexed { i, pt ->
                val x = ox + (pt.first - b.x0) * s
                val y = oy + (b.y1 - pt.second) * s
                if (i == 0) p.moveTo(x, y) else p.lineTo(x, y)
            }
            p.close()
            drawPath(p, fill)
            drawPath(p, line, style = Stroke(width = 1.5.dp.toPx()))
        }
    }
}

private class Bounds(val x0: Float, val x1: Float, val y0: Float, val y1: Float) {
    val w: Float get() = (x1 - x0).coerceAtLeast(1e-6f)
    val h: Float get() = (y1 - y0).coerceAtLeast(1e-6f)
}

private class Outline(val rings: List<List<Pair<Float, Float>>>, val bounds: Bounds)

/** Rings as (east, north) in kilometre-like units, so that the shape is not stretched. */
private fun outlineOf(t: TownArea): Outline {
    val kx = cos(t.lat * PI / 180).toFloat()
    val rings = t.rings.map { r -> (0 until r.size step 2).map { i -> (r[i + 1].toFloat() * kx) to r[i].toFloat() } }
    val xs = rings.flatten().map { it.first }
    val ys = rings.flatten().map { it.second }
    return Outline(rings, Bounds(xs.min(), xs.max(), ys.min(), ys.max()))
}

/** One postcard: the outline of the place, and either its name and figures or a riddle about it. */
@Composable
internal fun PostcardCard(t: TownArea, revealed: Boolean, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(4.dp)
    Row(
        modifier.fillMaxWidth().background(Vintage.parchmentLight, shape).border(1.dp, Vintage.edge, shape).padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(96.dp).background(Vintage.parchmentDark, RoundedCornerShape(3.dp)).padding(6.dp)) {
            TownShape(
                t, fill = if (revealed) Vintage.moss.copy(alpha = 0.55f) else Vintage.ink.copy(alpha = 0.4f),
                line = if (revealed) Vintage.moss else Vintage.ink, modifier = Modifier.size(84.dp),
            )
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            if (revealed) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(t.name, fontFamily = QUILL, fontSize = 24.sp, fontWeight = FontWeight.Black, color = INK_RED, modifier = Modifier.weight(1f))
                    Box(Modifier.border(1.dp, Vintage.brassDark, RoundedCornerShape(2.dp)).padding(horizontal = 5.dp, vertical = 2.dp)) {
                        Text(tr("郵"), fontFamily = QUILL, fontSize = 12.sp, color = Vintage.brassDark)
                    }
                }
                Text(t.county, fontFamily = QUILL, fontSize = 13.sp, color = Vintage.inkSoft)
                postcardBody(t).forEach { Text(it, fontFamily = QUILL, fontSize = 13.sp, color = SEPIA) }
            } else {
                Text("？？？", fontFamily = QUILL, fontSize = 24.sp, fontWeight = FontWeight.Black, color = Vintage.inkSoft)
                Text(postcardHint(t), fontFamily = QUILL, fontSize = 14.sp, color = SEPIA)
            }
        }
    }
}

@Composable
private fun ParchmentSheet(content: @Composable () -> Unit) {
    val shape = RoundedCornerShape(10.dp)
    Box(Modifier.fillMaxWidth().background(Vintage.parchment, shape).border(1.5.dp, Vintage.edge, shape).padding(16.dp)) { content() }
}

/** The postcard page: pick one of the offered mysteries (when a postcard is left), and look at the ones already collected. */
@Composable
internal fun PostcardsSheet(left: Int, offer: List<TownArea>, mine: List<TownArea>, onPick: (TownArea) -> Unit, onOpen: (TownArea) -> Unit, onClose: () -> Unit) {
    ParchmentSheet {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(tr("明信片"), fontFamily = QUILL, fontSize = 28.sp, fontWeight = FontWeight.Black, color = INK_RED)
            LazyColumn(Modifier.heightIn(max = 480.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (offer.isNotEmpty()) {
                    item { Text(trf("可以選 {0} 張。三選一，選了才揭曉是哪裡：", left), fontFamily = QUILL, fontSize = 15.sp, color = SEPIA) }
                    items(offer, key = { "offer_${it.county}|${it.name}" }) { t -> PostcardCard(t, revealed = false, Modifier.clickable { onPick(t) }) }
                } else {
                    item { Text(tr("目前沒有可用的明信片。完成「任務與里程碑」就能獲得。"), fontFamily = QUILL, fontSize = 15.sp, color = SEPIA) }
                }
                if (mine.isNotEmpty()) {
                    item { Text(trf("已收集 {0} 張", mine.size), fontFamily = QUILL, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Vintage.ink) }
                    items(mine, key = { "mine_${it.county}|${it.name}" }) { t ->
                        Row(Modifier.fillMaxWidth().clickable { onOpen(t) }.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            TownShape(t, Vintage.moss.copy(alpha = 0.55f), Vintage.moss, Modifier.size(40.dp))
                            Text("${t.county} ${t.name}", fontFamily = QUILL, fontSize = 16.sp, color = SEPIA)
                        }
                    }
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) { TextButton(onClick = onClose) { Text(tr("關閉"), fontFamily = QUILL, fontSize = 18.sp, fontWeight = FontWeight.Black) } }
        }
    }
}

/** The postcard that has just been picked, or one opened again from the list. */
@Composable
internal fun PostcardOpenedSheet(t: TownArea, onClose: () -> Unit) {
    ParchmentSheet {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(trf("來自{0}的明信片", t.name), fontFamily = QUILL, fontSize = 22.sp, fontWeight = FontWeight.Black, color = INK_RED)
            PostcardCard(t, revealed = true)
            Text(tr("這個地方不在你的環島路線上，是靠任務換來的一張明信片。"), fontFamily = QUILL, fontSize = 14.sp, color = Vintage.inkSoft)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) { TextButton(onClick = onClose) { Text(tr("收下"), fontFamily = QUILL, fontSize = 18.sp, fontWeight = FontWeight.Black) } }
        }
    }
}

@Composable
fun PostcardsDialog(vm: AppViewModel, ledger: Ledger, onDismiss: () -> Unit) {
    var opened by remember { mutableStateOf<TownArea?>(null) }
    val offer = remember(ledger.unlocked, ledger.earned, ledger.towns) { vm.postcardOffer(ledger).mapNotNull { vm.townByKey(it) } }
    val mine = remember(ledger.unlocked) { ledger.unlocked.sorted().mapNotNull { vm.townByKey(it) } }
    val show = opened
    Dialog(onDismissRequest = { if (show != null) opened = null else onDismiss() }) {
        if (show != null) PostcardOpenedSheet(show) { opened = null }
        else PostcardsSheet(
            ledger.postcardsLeft, offer, mine,
            onPick = { t -> vm.unlockPostcard("${t.county}|${t.name}"); opened = t },
            onOpen = { opened = it }, onClose = onDismiss,
        )
    }
}
