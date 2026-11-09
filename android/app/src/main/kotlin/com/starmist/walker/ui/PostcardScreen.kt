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
import com.starmist.core.route.PostcardTowns
import com.starmist.core.route.TownProfile
import com.starmist.core.route.Traveler
import com.starmist.core.route.Travelers
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

internal fun TownArea.profile() = TownProfile("$county|$name", county, name, highElevM, medianElevM, areaKm2, villages)

/** One postcard: the outline of the place, and either its name and figures (in the sender's words if [sender] is given) or a riddle about it. */
@Composable
internal fun PostcardCard(t: TownArea, revealed: Boolean, modifier: Modifier = Modifier, sender: Traveler? = null, fact: String? = null) {
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
                    if (sender != null) TravelerAvatar(sender.id, 36.dp)
                    else Box(Modifier.border(1.dp, Vintage.brassDark, RoundedCornerShape(2.dp)).padding(horizontal = 5.dp, vertical = 2.dp)) {
                        Text(tr("郵"), fontFamily = QUILL, fontSize = 12.sp, color = Vintage.brassDark)
                    }
                }
                Text(t.county, fontFamily = QUILL, fontSize = 13.sp, color = Vintage.inkSoft)
                if (sender != null) PostcardTowns.lines(sender, t.profile(), fact).forEach { Text(it, fontFamily = QUILL, fontSize = 13.sp, color = SEPIA) }
                else postcardBody(t).forEach { Text(it, fontFamily = QUILL, fontSize = 13.sp, color = SEPIA) }
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

/** One traveler in the list: 0 = not met yet, 1 = met, 2 = a friend. */
internal class TravelerRow(val traveler: Traveler, val state: Int)

/** The postcard page: the travelers (with a button to ask a friend to write when a quest postcard is left) and the postcards received. */
@Composable
internal fun PostcardsSheet(left: Int, travelers: List<TravelerRow>, mine: List<Pair<TownArea, Traveler?>>, onAsk: (Traveler) -> Unit, onOpen: (TownArea) -> Unit, onClose: () -> Unit) {
    ParchmentSheet {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(tr("旅人與明信片"), fontFamily = QUILL, fontSize = 28.sp, fontWeight = FontWeight.Black, color = INK_RED)
            LazyColumn(Modifier.heightIn(max = 520.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                item {
                    Text(
                        if (left > 0) trf("任務換到 {0} 張請託：選一位朋友，請他寄一張明信片。", left) else tr("路上遇到的旅人，成為朋友後會寄明信片給你。"),
                        fontFamily = QUILL, fontSize = 15.sp, color = SEPIA,
                    )
                }
                items(travelers, key = { "t_${it.traveler.id}" }) { r ->
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        TravelerAvatar(r.traveler.id, 52.dp, dim = r.state == 0)
                        Column(Modifier.weight(1f)) {
                            Text(if (r.state == 0) "？？？" else r.traveler.name, fontFamily = QUILL, fontSize = 17.sp, fontWeight = FontWeight.Bold, color = Vintage.ink)
                            Text(
                                when (r.state) { 0 -> tr("還沒遇到"); 1 -> trf("認識・{0}", r.traveler.job); else -> trf("朋友・{0}", r.traveler.job) },
                                fontFamily = QUILL, fontSize = 13.sp, color = Vintage.inkSoft,
                            )
                        }
                        if (r.state == 2 && left > 0) TextButton(onClick = { onAsk(r.traveler) }) { Text(tr("請他寄一張"), fontFamily = QUILL, fontSize = 15.sp, fontWeight = FontWeight.Black, color = INK_RED) }
                    }
                }
                if (mine.isNotEmpty()) {
                    item { Text(trf("收到的明信片 {0} 張", mine.size), fontFamily = QUILL, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Vintage.ink) }
                    items(mine, key = { "mine_${it.first.county}|${it.first.name}" }) { (t, from) ->
                        Row(Modifier.fillMaxWidth().clickable { onOpen(t) }.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            TownShape(t, Vintage.moss.copy(alpha = 0.55f), Vintage.moss, Modifier.size(40.dp))
                            Text("${t.county} ${t.name}", fontFamily = QUILL, fontSize = 16.sp, color = SEPIA, modifier = Modifier.weight(1f))
                            if (from != null) TravelerAvatar(from.id, 30.dp)
                        }
                    }
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) { TextButton(onClick = onClose) { Text(tr("關閉"), fontFamily = QUILL, fontSize = 18.sp, fontWeight = FontWeight.Black) } }
        }
    }
}

/** A postcard that has just come, or one opened again from the list. */
@Composable
internal fun PostcardOpenedSheet(t: TownArea, sender: Traveler?, fact: String? = null, onClose: () -> Unit) {
    ParchmentSheet {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(if (sender != null) trf("{0}寄來一張明信片", sender.name) else trf("來自{0}的明信片", t.name), fontFamily = QUILL, fontSize = 22.sp, fontWeight = FontWeight.Black, color = INK_RED)
            PostcardCard(t, revealed = true, sender = sender, fact = fact)
            Text(tr("這個地方不在你的環島路線上。"), fontFamily = QUILL, fontSize = 14.sp, color = Vintage.inkSoft)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) { TextButton(onClick = onClose) { Text(tr("收下"), fontFamily = QUILL, fontSize = 18.sp, fontWeight = FontWeight.Black) } }
        }
    }
}

/** What a traveler says when met (stage 1) or when they become a friend (stage 2). */
@Composable
internal fun TravelerMeetSheet(t: Traveler, stage: Int, onClose: () -> Unit) {
    ParchmentSheet {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(if (stage == 1) tr("路上遇到一位旅人") else tr("成為朋友"), fontFamily = QUILL, fontSize = 24.sp, fontWeight = FontWeight.Black, color = INK_RED)
            TravelerAvatar(t.id, 104.dp)
            Text(t.name, fontFamily = QUILL, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Vintage.ink)
            Text(t.job, fontFamily = QUILL, fontSize = 14.sp, color = Vintage.inkSoft)
            Text("「" + (if (stage == 1) t.hello else t.friend) + "」", fontFamily = QUILL, fontSize = 16.sp, color = SEPIA)
            if (stage == 2) Text(tr("之後會收到他從路線外寄來的明信片。"), fontFamily = QUILL, fontSize = 13.sp, color = Vintage.inkSoft)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) { TextButton(onClick = onClose) { Text(if (stage == 1) tr("你好") else tr("太好了"), fontFamily = QUILL, fontSize = 18.sp, fontWeight = FontWeight.Black) } }
        }
    }
}

@Composable
fun PostcardsDialog(vm: AppViewModel, ledger: Ledger, onDismiss: () -> Unit) {
    var opened by remember { mutableStateOf<TownArea?>(null) }
    val travelers = remember(ledger.met, ledger.friends) { Travelers.all.map { TravelerRow(it, if (it.id in ledger.friends) 2 else if (it.id in ledger.met) 1 else 0) } }
    val mine = remember(ledger.unlocked, ledger.senders) {
        ledger.unlocked.sorted().mapNotNull { k -> vm.townByKey(k)?.let { it to ledger.senders[k]?.let(Travelers::byId) } }
    }
    val show = opened
    Dialog(onDismissRequest = { if (show != null) opened = null else onDismiss() }) {
        if (show != null) {
            val from = ledger.senders["${show.county}|${show.name}"]
            PostcardOpenedSheet(show, from?.let(Travelers::byId), from?.let { vm.postcardFact("${show.county}|${show.name}", it) }) { opened = null }
        }
        else PostcardsSheet(ledger.postcardsLeft, travelers, mine, onAsk = { vm.requestPostcard(it.id); onDismiss() }, onOpen = { opened = it }, onClose = onDismiss)
    }
}

/** A postcard has arrived: shown over the map as soon as it is due. */
@Composable
fun PostcardArrivedDialog(vm: AppViewModel, travelerId: String, townKey: String, onDismiss: () -> Unit) {
    val t = vm.townByKey(townKey) ?: run { onDismiss(); return }
    Dialog(onDismissRequest = onDismiss) { PostcardOpenedSheet(t, Travelers.byId(travelerId), vm.postcardFact(townKey, travelerId), onDismiss) }
}

@Composable
fun TravelerMetDialog(travelerId: String, stage: Int, onDismiss: () -> Unit) {
    val t = Travelers.byId(travelerId) ?: run { onDismiss(); return }
    Dialog(onDismissRequest = onDismiss) { TravelerMeetSheet(t, stage, onDismiss) }
}
