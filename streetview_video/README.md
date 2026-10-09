# 路線街景影片系統

## 最簡單：純網頁版（`web/index.html`）
單一 HTML 檔，不需要 Python 或 ffmpeg，直接用瀏覽器開啟即可（或放到 GitHub Pages / 任何靜態空間）：

1. 填入 Mapillary API token
2. 在地圖點選或搜尋地址，設定起點與終點
3. 從最多 3 條候選路線中選一條
4. 按「產生影片」，完成後可預覽並下載（Chrome/Edge 為 mp4，其他瀏覽器為 webm）

注意：產生期間請保持分頁在前景（瀏覽器會暫停背景分頁的計時）；影片以即時速度錄製，時長約為 張數 ÷ 每秒張數。token 只存在你的瀏覽器，請求直接送往 Mapillary，不經任何伺服器。

---

# 伺服器版（Flask + ffmpeg）

輸入起點與終點（地圖點選或地址搜尋），系統用 OSRM 規劃路線，依序從 Mapillary 取得沿途街景，合成 mp4，可線上預覽與下載。

## 啟動
```bash
pip install -r requirements.txt     # 需另外安裝 ffmpeg
export MAPILLARY_TOKEN='MLY|...'    # 可省略，改在網頁上貼 token
python app.py                       # http://127.0.0.1:8000
```
Docker：`docker build -t svvideo . && docker run -p 8000:8000 -e MAPILLARY_TOKEN='MLY|...' -v svdata:/data svvideo`

## 使用
1. 點地圖選起點、終點（或輸入地址）；路線與預估影像張數會即時顯示。
2. 第一次建議在「進階設定」把最多幀數設 50 試跑。
3. 按「產生影片」，在右側任務清單看進度，完成後可播放與下載。

## 設定
環境變數：`MAPILLARY_TOKEN`、`SV_DATA_DIR`（輸出位置）、`SV_WORKERS`（同時任務數，預設 2）、`SV_MAX_FRAMES`（單次上限，預設 1500）、`HOST`/`PORT`。

## 注意
- 任務狀態存在記憶體，重啟後清單會清空；只保留最近 20 筆。
- 預設只綁 127.0.0.1。若要對外開放，請自行加上認證，否則他人可使用你的 token 與運算資源。
- 公開 OSRM 與 Nominatim 有使用限制，僅適合個人少量使用。
- 影像為 CC-BY-SA 4.0（Mapillary 貢獻者），分享影片請標註來源。

CLI：`MAPILLARY_TOKEN=... python route_video.py --start 25.03,121.56 --end 25.04,121.55 -o out.mp4`

測試：`pytest tests`
