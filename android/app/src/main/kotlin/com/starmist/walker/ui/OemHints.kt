package com.starmist.walker.ui

import android.os.Build
import java.util.Locale

/** Where to find the battery / auto-start switches on common brands. Menu names vary by version. */
object OemHints {
    fun forThisDevice(): String {
        val maker = Build.MANUFACTURER.lowercase(Locale.ROOT)
        return when {
            "samsung" in maker ->
                "三星：設定 → 電池 → 背景使用限制，把本 App 加進「永不休眠的應用程式」，並把本 App 的電池用量設為「不受限制」。"
            "xiaomi" in maker || "redmi" in maker || "poco" in maker ->
                "小米／紅米／POCO：設定 → 應用程式 → 本 App，開啟「自啟動」，並把「省電策略」改為「無限制」。"
            "oppo" in maker || "oneplus" in maker || "realme" in maker ->
                "OPPO／OnePlus／realme：設定 → 電池，允許本 App「自動啟動」和「背景運行」，不要讓系統自動優化。"
            "vivo" in maker || "iqoo" in maker ->
                "vivo／iQOO：設定 → 電池 → 後台耗電管理，允許本 App 後台高耗電，並開啟自啟動。"
            "huawei" in maker || "honor" in maker ->
                "華為／榮耀：設定 → 電池 → 啟動管理，把本 App 改為「手動管理」並開啟全部三個開關；最近工作畫面也可以把本 App 鎖定。"
            else ->
                "請在系統設定把本 App 的電池用量設為「不受限制」，並允許它在背景執行。各機型的選單名稱可能不同。"
        }
    }
}
