package com.starmist.walker.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.starmist.core.route.GeoPoint
import com.starmist.core.route.Landmark
import com.starmist.core.route.RouteDef
import com.starmist.core.route.RouteEvent
import com.starmist.core.route.TaiwanOutline
import kotlin.math.cos
import kotlin.math.roundToInt

private const val LON_MIN = 119.95
private const val LON_MAX = 122.1
private const val LAT_MIN = 21.8
private const val LAT_MAX = 25.4
private val STAMP_INK = Color(0xFFB04A3A)
private val SUPPLY_ORANGE = Color(0xFFD9744B)

/** The island walk as a map the player can pinch and drag; it opens on the walker and cannot zoom out past the whole island. */
@Composable
fun RouteMapView(route: RouteDef, walkedKm: Double, modifier: Modifier = Modifier, initialZoom: Float? = null) {
    val measurer = rememberMapMeasurer()
    val data = rememberRealMapData(route)
    val pin = rememberArt("traveller_fox")
    val base = rememberArt("real_map_base")
    val tiles = rememberMapTiles()
    val areas = rememberTownAreas()
    val explored = remember(areas, route, (walkedKm * 2).toInt()) { areas.visited(route, walkedKm).keys }
    val currentTown = remember(areas, route, (walkedKm * 2).toInt()) { route.positionAt(walkedKm).let { areas.find(it.lat, it.lng) } }
    val here = route.positionAt(walkedKm)
    ZoomPanMap(
        aspect = REAL_MAP_ASPECT,
        focus = here.unit(),
        followKey = (walkedKm * 20).toInt(),
        modifier = modifier,
        initialZoom = initialZoom ?: if (base != null) 5.5f else 12f,
        maxZoom = 12f,
        background = SEA_COLOR,
    ) { w, h, _, view -> drawRealMap(w, h, route, data, walkedKm, measurer, pin, base, tiles, view, areas, explored, currentTown) }
}

/** Small round picture for a card: the painted card art if the asset library has it, a letter tile otherwise. */
@Composable
fun CardEmblem(card: Landmark, owned: Boolean, modifier: Modifier = Modifier) {
    val art = rememberArt("card_${card.id}")
    if (owned && art != null) {
        Image(art, contentDescription = card.name, modifier = modifier.clip(CircleShape), contentScale = ContentScale.Crop)
    } else {
        Box(
            modifier.clip(CircleShape).background(if (owned) Vintage.mossLight else Vintage.edge.copy(alpha = 0.5f)),
            contentAlignment = Alignment.Center,
        ) {
            Text(if (owned) card.name.take(1) else "？", fontSize = 22.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Serif, color = Vintage.ink)
        }
    }
}

/** A collected card, large: picture, the ten-character line, and where on the route it was found. */
@Composable
fun CardFace(card: Landmark, atKm: Double) {
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        val art = rememberArt("card_${card.id}")
        if (art != null) {
            Image(art, contentDescription = card.name, modifier = Modifier.fillMaxWidth().aspectRatio(4f / 3f).clip(RoundedCornerShape(10.dp)), contentScale = ContentScale.Crop)
        } else {
            CardEmblem(card, true, Modifier.size(96.dp))
        }
        Text(card.blurb, fontSize = 20.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Serif, color = Vintage.ink)
        Text("路線第 ${atKm.roundToInt()} 公里", style = MaterialTheme.typography.labelMedium)
    }
}

/** A round station stamp: station name and the year the station opened. */
@Composable
fun StationStamp(name: String, opened: String, modifier: Modifier = Modifier) {
    val measurer = rememberTextMeasurer()
    val big = TextStyle(fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = STAMP_INK)
    val small = TextStyle(fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = STAMP_INK)
    Canvas(modifier.aspectRatio(1f)) {
        val c = Offset(size.width / 2, size.height / 2)
        val r = size.minDimension / 2 - 2.dp.toPx()
        rotate(-8f, c) {
            drawCircle(STAMP_INK.copy(alpha = 0.9f), r, c, style = Stroke(3.dp.toPx()))
            drawCircle(STAMP_INK.copy(alpha = 0.7f), r - 6.dp.toPx(), c, style = Stroke(1.dp.toPx()))
            val top = measurer.measure("台鐵車站", small)
            drawText(top, topLeft = Offset(c.x - top.size.width / 2f, c.y - r * 0.62f))
            val title = measurer.measure(name, big)
            drawText(title, topLeft = Offset(c.x - title.size.width / 2f, c.y - title.size.height / 2f))
            val year = measurer.measure("$opened 年開站", small)
            drawText(year, topLeft = Offset(c.x - year.size.width / 2f, c.y + r * 0.38f))
        }
    }
}

/** One thing that happened on the route, shown when it is reached. */
@Composable
fun RouteEventDialog(event: RouteEvent, onDismiss: () -> Unit) {
    when (event) {
        is RouteEvent.CardCollected -> AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("獲得景點卡：${event.landmark.name}") },
            text = { CardFace(event.landmark, event.atKm) },
            confirmButton = { TextButton(onClick = onDismiss) { Text("收下") } },
        )
        is RouteEvent.StampCollected -> AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("蓋上紀念章") },
            text = {
                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    StationStamp(event.supply.name, event.supply.opened, Modifier.size(170.dp))
                    Text("${event.supply.name}・${event.supply.opened} 年開站", style = MaterialTheme.typography.bodyMedium)
                }
            },
            confirmButton = { TextButton(onClick = onDismiss) { Text("收下") } },
        )
        is RouteEvent.Transit -> AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("${event.by}前往${event.into}") },
            text = { Text("這一段不能步行，直接${event.by}抵達，不計步數。") },
            confirmButton = { TextButton(onClick = onDismiss) { Text("上車") } },
        )
        RouteEvent.Finished -> AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("環島完成！") },
            text = { Text("你用自己的腳步繞完了整座島。景點卡和紀念章都留在「路線」頁。") },
            confirmButton = { TextButton(onClick = onDismiss) { Text("太棒了") } },
        )
    }
}
