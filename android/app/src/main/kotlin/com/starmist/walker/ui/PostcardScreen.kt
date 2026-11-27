package com.starmist.walker.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.starmist.core.route.PostcardFact
import com.starmist.core.route.PostcardTowns
import com.starmist.core.route.Traveler
import com.starmist.core.route.Travelers
import com.starmist.walker.data.Ledger
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min

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

/** False in the screenshot tests while a picture is not in the project yet (the preview tool answers any picture name with a made-up id). */
internal val LocalPostcardArt = androidx.compose.runtime.staticCompositionLocalOf { true }

/** The picture of a postcard (res/drawable-nodpi/postcard_<art>.webp); when it is not there yet, the outline of the place on paper. */
@Composable
internal fun PostcardPicture(t: TownArea, art: String?, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val enabled = LocalPostcardArt.current
    val resId = remember(art, enabled) { if (art == null || !enabled) 0 else drawableId(context, "postcard_$art") }
    val shape = RoundedCornerShape(3.dp)
    if (resId != 0) {
        Image(painterResource(resId), contentDescription = null, contentScale = ContentScale.Crop, modifier = modifier.clip(shape).border(1.dp, Vintage.edge, shape))
    } else {
        Box(modifier.background(Vintage.parchmentDark, shape).border(1.dp, Vintage.edge, shape), contentAlignment = Alignment.Center) {
            TownShape(t, Vintage.moss.copy(alpha = 0.45f), Vintage.moss, Modifier.fillMaxSize(0.7f))
        }
    }
}

@Composable
private fun ParchmentSheet(content: @Composable () -> Unit) {
    Box(Modifier.padding(14.dp)) {
        PaperCard(Modifier.fillMaxWidth(), Lift.Lifted, 14.dp, tint = Vintage.parchment) { Box(Modifier.padding(16.dp)) { content() } }
    }
}

/** One traveler in the list: 0 = not met yet, 1 = met, 2 = a friend. */
internal class TravelerRow(val traveler: Traveler, val state: Int, val waiting: Boolean = false, val points: Int = 0)

/** A postcard received: the place, who wrote it, and what they wrote (with the picture). */
internal class MailRow(val town: TownArea, val from: Traveler?, val fact: PostcardFact?)

/** The postcard page: the travelers (with a button to ask a friend to write when a quest postcard is left) and the postcards received. */
@Composable
internal fun PostcardsSheet(left: Int, travelers: List<TravelerRow>, mine: List<MailRow>, onSend: (Traveler) -> Unit, onStories: (Traveler) -> Unit, onOpen: (TownArea) -> Unit, onClose: () -> Unit) {
    ParchmentSheet {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(tr("旅人與明信片"), fontFamily = QUILL, fontSize = 28.sp, fontWeight = FontWeight.Black, color = INK_RED)
            LazyColumn(Modifier.heightIn(max = 520.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                item {
                    Text(
                        if (left > 0) trf("你有 {0} 張郵票：選一位朋友，寄張明信片給他。", left) else tr("成為朋友後，可以用郵票寄明信片給他；完成任務就能得到郵票。"),
                        fontFamily = QUILL, fontSize = 15.sp, color = SEPIA,
                    )
                }
                items(travelers, key = { "t_${it.traveler.id}" }) { r ->
                    PaperCard(Modifier.fillMaxWidth(), if (r.state == 0) Lift.Rest else Lift.Raised, 10.dp, onClick = if (r.state == 2 && r.points >= Travelers.STORY_AT.first()) ({ onStories(r.traveler) }) else null) {
                    Row(Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        TravelerAvatar(r.traveler.id, 52.dp, dim = r.state == 0)
                        Column(Modifier.weight(1f)) {
                            Text(if (r.state == 0) "？？？" else r.traveler.name, fontFamily = QUILL, fontSize = 17.sp, fontWeight = FontWeight.Bold, color = Vintage.ink)
                            Text(
                                when (r.state) { 0 -> tr("還沒遇到"); 1 -> trf("認識・{0}", r.traveler.job); else -> trf("朋友・{0}", r.traveler.job) },
                                fontFamily = QUILL, fontSize = 13.sp, color = Vintage.inkSoft,
                            )
                            if (r.state == 2 && r.points >= Travelers.STORY_AT.first()) Text(trf("小故事 {0} 則（點開來看）", Travelers.STORY_AT.count { it <= r.points }), fontFamily = QUILL, fontSize = 12.sp, color = INK_RED)
                        }
                        if (r.state == 2) {
                            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                if (r.points > 0) Text(trf("友誼 ♥{0}", r.points), fontFamily = QUILL, fontSize = 12.sp, color = INK_RED)
                                if (r.waiting) Text(tr("等他回信中…"), fontFamily = QUILL, fontSize = 12.sp, color = Vintage.inkSoft)
                                else if (left > 0) BrassPill(tr("寄明信片"), { onSend(r.traveler) })
                            }
                        }
                    }
                    }
                }
                if (mine.isNotEmpty()) {
                    item { Text(trf("收到的明信片 {0} 張", mine.size), fontFamily = QUILL, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Vintage.ink) }
                    items(mine, key = { "mine_${it.town.county}|${it.town.name}" }) { r ->
                        PaperCard(Modifier.fillMaxWidth(), Lift.Raised, 10.dp, onClick = { onOpen(r.town) }) {
                        Row(Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            PostcardPicture(r.town, r.fact?.art, Modifier.size(width = 60.dp, height = 40.dp))
                            Text("${r.town.county} ${r.town.name}", fontFamily = QUILL, fontSize = 16.sp, color = SEPIA, modifier = Modifier.weight(1f))
                            if (r.from != null) TravelerAvatar(r.from.id, 30.dp)
                        }
                        }
                    }
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) { BrassPill(tr("關閉"), onClose) }
        }
    }
}

/** Choosing what to write on the postcard to a friend: three sentences, each the way you would write to that friend. */
@Composable
internal fun SendPostcardSheet(t: Traveler, onPick: (Int) -> Unit, onClose: () -> Unit) {
    ParchmentSheet {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                TravelerAvatar(t.id, 52.dp)
                Text(trf("寄明信片給{0}", t.name), fontFamily = QUILL, fontSize = 22.sp, fontWeight = FontWeight.Black, color = INK_RED)
            }
            Text(tr("明信片會寫上你現在走到的地方。選一句話寫在上面："), fontFamily = QUILL, fontSize = 14.sp, color = Vintage.inkSoft)
            Travelers.cheers(t.id).forEachIndexed { i, line ->
                PaperCard(Modifier.fillMaxWidth(), Lift.Raised, 10.dp, onClick = { onPick(i) }) {
                    Text("「$line」", fontFamily = QUILL, fontSize = 15.sp, lineHeight = 22.sp, color = SEPIA, modifier = Modifier.padding(12.dp))
                }
            }
            Text(trf("寄出要用一張郵票。走大約 {0} 公里後，他會回信。", Travelers.REPLY_KM.toInt()), fontFamily = QUILL, fontSize = 12.sp, color = Vintage.inkSoft)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) { BrassPill(tr("取消"), onClose) }
        }
    }
}

/** The stories a friend has told so far (one for each step of friendship), and what the next one needs. */
@Composable
internal fun StorySheet(t: Traveler, points: Int, onClose: () -> Unit) {
    ParchmentSheet {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                TravelerAvatar(t.id, 52.dp)
                Column {
                    Text(trf("{0}的小故事", t.name), fontFamily = QUILL, fontSize = 22.sp, fontWeight = FontWeight.Black, color = INK_RED)
                    Text(trf("友誼 ♥{0}", points), fontFamily = QUILL, fontSize = 13.sp, color = Vintage.inkSoft)
                }
            }
            Travelers.stories(t.id).forEachIndexed { i, story ->
                val need = Travelers.STORY_AT[i]
                PaperCard(Modifier.fillMaxWidth(), if (points >= need) Lift.Raised else Lift.Rest, 10.dp) {
                    Text(
                        if (points >= need) story else trf("友誼 ♥{0} 時解鎖", need),
                        fontFamily = QUILL, fontSize = 15.sp, lineHeight = 23.sp, color = if (points >= need) SEPIA else Vintage.inkSoft, modifier = Modifier.padding(12.dp),
                    )
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) { BrassPill(tr("關閉"), onClose) }
        }
    }
}

/** A postcard that has just come, or one opened again from the list. */
@Composable
internal fun PostcardOpenedSheet(t: TownArea, sender: Traveler, fact: PostcardFact?, onClose: () -> Unit) {
    ParchmentSheet {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(trf("{0}寄來一張明信片", sender.name), fontFamily = QUILL, fontSize = 22.sp, fontWeight = FontWeight.Black, color = INK_RED)
            PostcardCard(t, sender, fact)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) { BrassPill(tr("收下"), onClose) }
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
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) { BrassPill(if (stage == 1) tr("你好") else tr("太好了"), onClose) }
        }
    }
}

@Composable
fun PostcardsDialog(vm: AppViewModel, ledger: Ledger, onDismiss: () -> Unit) {
    var opened by remember { mutableStateOf<TownArea?>(null) }
    var sending by remember { mutableStateOf<Traveler?>(null) }
    var reading by remember { mutableStateOf<Traveler?>(null) }
    val travelers = remember(ledger.met, ledger.friends, ledger.pending, ledger.friendship) {
        Travelers.all.map { TravelerRow(it, if (it.id in ledger.friends) 2 else if (it.id in ledger.met) 1 else 0, it.id in ledger.waiting, ledger.friendship[it.id] ?: 0) }
    }
    val mine = remember(ledger.unlocked, ledger.senders) {
        ledger.unlocked.sorted().mapNotNull { k ->
            vm.townByKey(k)?.let { t -> val from = ledger.senders[k]; MailRow(t, from?.let(Travelers::byId), from?.let { vm.postcardFact(k, it) }) }
        }
    }
    val show = opened
    val key = show?.let { "${it.county}|${it.name}" }
    val from = key?.let { ledger.senders[it] }?.let(Travelers::byId)
    val to = sending
    val story = reading
    Dialog(onDismissRequest = { if (story != null) reading = null else if (to != null) sending = null else if (show != null) opened = null else onDismiss() }) {
        if (story != null) StorySheet(story, ledger.friendship[story.id] ?: 0) { reading = null }
        else if (to != null) SendPostcardSheet(to, onPick = { vm.sendPostcard(to.id); onDismiss() }, onClose = { sending = null })
        else if (show != null && key != null && from != null) PostcardOpenedSheet(show, from, vm.postcardFact(key, from.id)) { opened = null }
        else PostcardsSheet(ledger.postcardsLeft, travelers, mine, onSend = { sending = it }, onStories = { reading = it }, onOpen = { opened = it }, onClose = onDismiss)
    }
}

/** A postcard has arrived: shown over the map as soon as it is due. */
@Composable
fun PostcardArrivedDialog(vm: AppViewModel, travelerId: String, townKey: String, onDismiss: () -> Unit) {
    val t = vm.townByKey(townKey) ?: run { onDismiss(); return }
    val from = Travelers.byId(travelerId) ?: run { onDismiss(); return }
    Dialog(onDismissRequest = onDismiss) { PostcardOpenedSheet(t, from, vm.postcardFact(townKey, travelerId), onDismiss) }
}

@Composable
fun TravelerMetDialog(travelerId: String, stage: Int, onDismiss: () -> Unit) {
    val t = Travelers.byId(travelerId) ?: run { onDismiss(); return }
    Dialog(onDismissRequest = onDismiss) { TravelerMeetSheet(t, stage, onDismiss) }
}
