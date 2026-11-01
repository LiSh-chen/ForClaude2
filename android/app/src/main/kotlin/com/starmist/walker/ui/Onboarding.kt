package com.starmist.walker.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.starmist.core.metrics.Sex
import com.starmist.walker.data.UserSettings

/** The privacy policy as the app states it. The same words are in docs/PRIVACY.md for the store page. */
internal val PRIVACY_PARAGRAPHS: List<Pair<String, String>> = listOf(
    "這個 App 做什麼" to "來台灣走走把你每天走的步數，換算成沿著環島路線前進的距離。",
    "用到哪些資料" to "步數：向手機的「動作與健身」權限讀取手機內建的計步器。\n你填的身高、體重、性別和每日目標：只用來估算步長、距離與熱量。\n你的進度：走到哪裡、收集了哪些卡片、印章和鄉鎮特色。",
    "資料放在哪裡" to "全部只存在你的手機裡。App 沒有帳號、沒有伺服器，也沒有申請網路權限，所以這些資料不會被傳出去，我們也看不到。",
    "我們不做的事" to "不蒐集位置、聯絡人、相機、麥克風或廣告識別碼；不放廣告；不使用第三方分析工具；不賣、不分享任何資料。",
    "通知" to "計步用的常駐通知和每日目標通知，只在你的手機上顯示。",
    "備份與刪除" to "匯出的備份檔由你自己決定放在哪裡、給誰。解除安裝 App，或在系統設定裡清除 App 的儲存空間，就會刪除 App 裡所有的資料。",
    "最後更新" to "2026 年 11 月 1 日。若有改變會寫在這裡。",
)

/** The privacy policy, on a sheet of parchment over the whole screen. */
@Composable
internal fun PrivacyDialog(onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) { PrivacySheet(onDismiss) }
}

/** The sheet itself (apart from the dialog it is shown in). */
@Composable
internal fun PrivacySheet(onDismiss: () -> Unit) {
    Box(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(12.dp)) {
        Canvas(Modifier.fillMaxSize()) { Pen(this, size).parchmentPage(30.dp.toPx(), 12.dp.toPx(), -1) }
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(start = 36.dp, end = 20.dp, top = 12.dp, bottom = 12.dp)) {
            Text("隱私權政策", fontFamily = QUILL, fontSize = 24.sp, fontWeight = FontWeight.Black, color = SEPIA)
            Spacer(Modifier.height(10.dp))
            PRIVACY_PARAGRAPHS.forEach { (head, body) ->
                Text(head, fontFamily = QUILL, fontSize = 17.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0058C8))
                Text(body, fontFamily = QUILL, fontSize = 14.sp, color = SEPIA, lineHeight = 22.sp, modifier = Modifier.padding(top = 2.dp, bottom = 10.dp))
            }
            TextButton(onClick = onDismiss, Modifier.align(Alignment.End)) { Text("關閉") }
        }
    }
}

private const val PAGES = 5

/**
 * What a new user sees once: what the app is, how it plays, why it asks for the step-counter permission, their body measures and goal, and a send-off. Each page is
 * a sheet of parchment. [onFinish] is called when they finish or skip.
 */
@Composable
internal fun OnboardingScreen(
    settings: UserSettings, onUpdate: ((UserSettings) -> UserSettings) -> Unit, permissionGranted: Boolean, onRequestPermission: () -> Unit, onFinish: () -> Unit, startPage: Int = 0,
) {
    var page by rememberSaveable { mutableIntStateOf(startPage) }
    val head = TextStyle(fontFamily = QUILL, fontSize = 26.sp, fontWeight = FontWeight.Black, color = SEPIA)
    val body = TextStyle(fontFamily = QUILL, fontSize = 16.sp, color = SEPIA, lineHeight = 26.sp)
    Box(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(14.dp)) {
        Canvas(Modifier.fillMaxSize()) { Pen(this, size).parchmentPage(26.dp.toPx(), 14.dp.toPx(), -1) }
        Column(Modifier.fillMaxSize().padding(start = 38.dp, end = 20.dp, top = 14.dp, bottom = 12.dp)) {
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                when (page) {
                    0 -> {
                        Text("歡迎來到\n來台灣走走", style = head)
                        Spacer(Modifier.height(14.dp))
                        Text("把每天走的路，變成一圈環島。", style = body.copy(fontWeight = FontWeight.Bold))
                        Spacer(Modifier.height(8.dp))
                        Text("你走的步數會沿著真實的環島路線前進；走過的鄉鎮會留下特色，路上的景點會給你卡片和印章。", style = body)
                    }
                    1 -> {
                        Text("怎麼走", style = head)
                        Spacer(Modifier.height(14.dp))
                        Text("步數依你的步長換成公里，地圖上的小旅人就跟著往前走。", style = body)
                        Spacer(Modifier.height(8.dp))
                        Text("點地圖上走過的鄉鎮，看那裡的特色；底部的按鈕可以開關景點、路線和區域的顯示。", style = body)
                        Spacer(Modifier.height(8.dp))
                        Text("地圖下方可以在設定裡換成儀表或筆記頁，字體也能調大小。", style = body)
                    }
                    2 -> {
                        Text("計步需要的許可", style = head)
                        Spacer(Modifier.height(14.dp))
                        Text("App 要用「動作與健身」權限，讀取手機內建的計步器。只讀步數，不會取得位置、聯絡人或其他資料，資料也不會離開手機。", style = body)
                        Spacer(Modifier.height(8.dp))
                        Text("想讓螢幕關閉時也持續計步，之後會請你允許一個常駐通知。", style = body)
                        Spacer(Modifier.height(16.dp))
                        if (permissionGranted) Text("已允許，可以計步了。", style = body.copy(fontWeight = FontWeight.Bold, color = Color(0xFF2E7D4F)))
                        else Button(onClick = onRequestPermission) { Text("允許動作與健身") }
                    }
                    3 -> {
                        Text("你的資料", style = head)
                        Spacer(Modifier.height(10.dp))
                        Text("用來估算步長、距離和熱量，之後可以在設定修改。", style = body)
                        Spacer(Modifier.height(10.dp))
                        NumberField("身高（公分）", settings.heightCm.toInt().toString(), KeyboardType.Number) { t ->
                            t.toDoubleOrNull()?.takeIf { it in 100.0..230.0 }?.let { h -> onUpdate { it.copy(heightCm = h) } }
                        }
                        NumberField("體重（公斤）", settings.weightKg.toInt().toString(), KeyboardType.Number) { t ->
                            t.toDoubleOrNull()?.takeIf { it in 25.0..250.0 }?.let { w -> onUpdate { it.copy(weightKg = w) } }
                        }
                        NumberField("每日目標（步）", settings.dailyGoal.toString(), KeyboardType.Number) { t ->
                            t.toIntOrNull()?.takeIf { it in 500..100_000 }?.let { g -> onUpdate { it.copy(dailyGoal = g) } }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 6.dp)) {
                            listOf(Sex.MALE to "男", Sex.FEMALE to "女", Sex.OTHER to "不指定").forEach { (value, label) ->
                                FilterChip(selected = settings.sex == value, onClick = { onUpdate { it.copy(sex = value) } }, label = { Text(label) })
                            }
                        }
                    }
                    else -> {
                        Text("出發！", style = head)
                        Spacer(Modifier.height(14.dp))
                        Text("到「今日」頁選一條路線，開始你的環島。", style = body)
                        Spacer(Modifier.height(8.dp))
                        Text("走到每日目標時，會通知你一次。", style = body)
                    }
                }
            }
            Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                if (page < PAGES - 1) TextButton(onClick = onFinish) { Text("略過") } else Spacer(Modifier.size(8.dp))
                Spacer(Modifier.weight(1f))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    repeat(PAGES) { i -> Canvas(Modifier.size(9.dp)) { drawCircle(if (i == page) Color(0xFF7C5A1B) else Color(0x557C5A1B), size.minDimension * (if (i == page) 0.5f else 0.33f)) } }
                }
                Spacer(Modifier.weight(1f))
                if (page > 0) TextButton(onClick = { page-- }) { Text("上一步") }
                Button(onClick = { if (page < PAGES - 1) page++ else onFinish() }) { Text(if (page < PAGES - 1) "下一步" else "開始") }
            }
        }
    }
}

/** What the letter at the end of the route tells. */
class FinishStats(val routeName: String, val totalKm: Double, val cards: Int, val stations: Int, val sights: Int, val towns: Int)

/** A letter on parchment in the middle of the screen: a title, some paragraphs on the ruled lines, and one button that closes it. */
@Composable
internal fun LetterDialog(title: String, paragraphs: List<String>, button: String, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) { LetterSheet(title, paragraphs, button, onDismiss) }
}

@Composable
internal fun LetterSheet(title: String, paragraphs: List<String>, button: String, onDismiss: () -> Unit) {
    val rowSp = with(androidx.compose.ui.platform.LocalDensity.current) { LETTER_ROW.toSp() }
    val onRule = TextStyle(
        fontFamily = QUILL, fontSize = 16.sp, color = SEPIA, lineHeight = rowSp,
        lineHeightStyle = androidx.compose.ui.text.style.LineHeightStyle(androidx.compose.ui.text.style.LineHeightStyle.Alignment.Center, androidx.compose.ui.text.style.LineHeightStyle.Trim.None),
    )
    Box(Modifier.fillMaxWidth()) {
        Canvas(Modifier.matchParentSize()) { Pen(this, size).parchmentPage(LETTER_ROW.toPx(), 10.dp.toPx(), -1) }
        Column(Modifier.fillMaxWidth().padding(start = 34.dp, end = 18.dp, top = 10.dp, bottom = 8.dp)) {
            Box(Modifier.height(LETTER_ROW * 2), contentAlignment = Alignment.CenterStart) {
                Text(title, fontFamily = QUILL, fontSize = 28.sp, fontWeight = FontWeight.Black, color = INK_RED)
            }
            paragraphs.forEach { p ->
                Text(p, style = onRule)
                Spacer(Modifier.height(LETTER_ROW))
            }
            Box(Modifier.fillMaxWidth().heightIn(min = 48.dp), contentAlignment = Alignment.CenterEnd) { TextButton(onClick = onDismiss) { Text(button, fontFamily = QUILL, fontSize = 18.sp, fontWeight = FontWeight.Black) } }
        }
    }
}
