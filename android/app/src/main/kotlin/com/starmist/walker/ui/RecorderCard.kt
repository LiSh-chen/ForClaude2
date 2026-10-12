package com.starmist.walker.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.starmist.walker.container
import java.io.File

private val LABELS = listOf("慢走", "一般走", "快走", "小跑", "停住", "上下樓梯", "紅綠燈等待")

/** Test builds: record when every step happens (also with the screen off), then share the file. */
@Composable
fun RecorderCard() {
    val context = LocalContext.current
    val recorder = remember { context.container.rhythmRecorder }
    val state by recorder.state.collectAsStateWithLifecycle()
    var files by remember { mutableStateOf(recorder.files()) }
    var refresh by remember { mutableIntStateOf(0) }
    var notice by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(state.running, refresh) { files = recorder.files() }

    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("走路節奏錄製（給作曲用）", fontWeight = FontWeight.SemiBold)
            Text(
                "按開始後正常走路，螢幕可以關掉；系統會把每一步的時間存進檔案，只含時間，沒有位置。" +
                    "請開著「穩定計步模式」，並在走的過程中偶爾按下面的標籤，標示現在是什麼走法。走完按停止，再分享檔案給我。",
                style = MaterialTheme.typography.bodySmall,
            )
            Text(
                if (state.running) "錄製中：已記錄 ${state.steps} 步（${state.batches} 批）" else "尚未錄製",
                fontWeight = FontWeight.SemiBold,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = { notice = if (recorder.start()) null else "無法開始：沒有步伐偵測感測器，或沒有授予權限" },
                    enabled = !state.running,
                ) { Text("開始") }
                OutlinedButton(onClick = { recorder.stop(); refresh++ }, enabled = state.running) { Text("停止") }
            }
            notice?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error) }
            if (state.running) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    LABELS.forEach { label -> OutlinedButton(onClick = { recorder.mark(label) }) { Text(label) } }
                }
            }
            if (files.isNotEmpty()) {
                Text("已錄的檔案", fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 8.dp))
                files.take(8).forEach { file ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("${file.name.removePrefix("rec_").removeSuffix(".txt")}（${file.length() / 1024} KB）", style = MaterialTheme.typography.bodySmall)
                        Row {
                            TextButton(onClick = { share(context, file) }) { Text("分享") }
                            TextButton(onClick = { notice = copy(context, file) }) { Text("複製") }
                            TextButton(onClick = { file.delete(); refresh++ }) { Text("刪除") }
                        }
                    }
                }
            }
        }
    }
}

private fun share(context: Context, file: File) {
    val uri = FileProvider.getUriForFile(context, context.packageName + ".files", file)
    val send = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(send, "分享走路資料").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
}

private fun copy(context: Context, file: File): String {
    val text = file.readText()
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText("starmist-rhythm", text))
    return "已複製 ${text.length / 1024} KB 到剪貼簿，貼到對話裡即可"
}
