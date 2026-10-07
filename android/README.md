# 星霧大陸（StarMist Walker）

Android 計步 App：準確、省電、只要「動作與健身」權限，並以魔法奇幻世界作為長期動力。設計見 [docs/DESIGN.md](docs/DESIGN.md)。

## 結構
- `core/` — 純 Kotlin（JVM）：差值快照、跨日分配、修正係數、備援步伐偵測器、距離/卡路里、週月年彙總、診斷判斷。有單元測試。
- `app/` — Android 應用（Compose、Room、DataStore、WorkManager）。

## 建置與測試
```bash
cd android
./gradlew -p core test          # 核心邏輯測試（不需要 Android SDK）
./gradlew :app:assembleDebug    # 需要 Android SDK（JDK 17+）
```
用 Android Studio 開啟 `android/` 資料夾即可。

## 目前進度（里程碑 1）
- 完成：計步核心與測試、首頁、週/月/年統計、設定（身高體重目標）、修正係數、校準精靈、手動補登、步長校準、診斷頁、背景讀取（WorkManager + 開機）。
- 尚未：世界與地圖（M2+）、GPS 軌跡（M4）、加速度計備援在 App 內的接線（偵測器已在 core 並有測試）、車輛過濾。
