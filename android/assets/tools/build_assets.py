#!/usr/bin/env python3
"""Asset library tool: check what exists, write the image-AI prompts, and export to the app.

    python3 tools/build_assets.py check      # which assets exist / are missing / look wrong
    python3 tools/build_assets.py prompts    # write PROMPTS.md (one ready-to-paste prompt per asset)
    python3 tools/build_assets.py export     # convert generated/*.png|jpg|webp -> app resources
    python3 tools/build_assets.py sheet      # contact sheet of everything present (preview.png)

Put finished images in generated/<id>.png (or .jpg/.webp). Export knocks the plain background out of
images that should be transparent, resizes them to the size in manifest.json, and writes
android/app/src/main/res/drawable-nodpi/art_<id>.webp. The app loads art by name and falls back to its
built-in drawings for anything that is missing, so assets can arrive one at a time.
"""
import json
import sys
from pathlib import Path

import numpy as np
from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parent.parent
MANIFEST = json.loads((ROOT / "manifest.json").read_text(encoding="utf-8"))
SOURCES = ROOT / "generated"
RES = ROOT.parent / "app" / "src" / "main" / "res"
DRAWABLE = RES / "drawable-nodpi"
EXTENSIONS = (".png", ".webp", ".jpg", ".jpeg")


def find_source(asset_id: str):
    for ext in EXTENSIONS:
        path = SOURCES / f"{asset_id}{ext}"
        if path.exists():
            return path
    return None


def knock_out_background(image: Image.Image, low: float = 18.0, high: float = 60.0, interior: bool = False) -> Image.Image:
    """Makes the flat background transparent. The colour is taken from the four corners.

    Only background connected to the border is removed, so white highlights inside the drawing stay.
    With interior=True every pixel of that colour goes (for art with an intentional opening, such as
    the compass case).
    """
    rgb = np.asarray(image.convert("RGB"), dtype=np.float64)
    h, w, _ = rgb.shape
    corners = np.array([rgb[0, 0], rgb[0, w - 1], rgb[h - 1, 0], rgb[h - 1, w - 1]])
    background = np.median(corners, axis=0)
    distance = np.sqrt(((rgb - background) ** 2).sum(axis=2))
    soft = np.clip((distance - low) / (high - low), 0, 1)
    if interior:
        alpha = soft
    else:
        # .copy(): an image made straight from a numpy array shares its memory and cannot be flood-filled.
        like_background = Image.fromarray(((distance < high) * 255).astype(np.uint8), "L").copy()
        seeds = [(0, 0), (w - 1, 0), (0, h - 1), (w - 1, h - 1), (w // 2, 0), (w // 2, h - 1), (0, h // 2), (w - 1, h // 2)]
        for seed in seeds:
            if like_background.getpixel(seed) == 255:
                ImageDraw.floodfill(like_background, seed, 128)
        connected = np.asarray(like_background) == 128
        alpha = np.where(connected, soft, 1.0)
    out = np.dstack([rgb, alpha * 255]).astype(np.uint8)
    return Image.fromarray(out, "RGBA")


def fit(image: Image.Image, size) -> Image.Image:
    """Scales down to the target size, cropping from the centre if the shape differs."""
    target_w, target_h = size
    w, h = image.size
    scale = max(target_w / w, target_h / h)
    if scale < 1:  # never upscale; only shrink
        image = image.resize((max(1, round(w * scale)), max(1, round(h * scale))), Image.LANCZOS)
    else:
        scale = 1
    w, h = image.size
    # crop to the target aspect ratio
    want = target_w / target_h
    if abs(w / h - want) > 0.01:
        if w / h > want:
            new_w = round(h * want)
            left = (w - new_w) // 2
            image = image.crop((left, 0, left + new_w, h))
        else:
            new_h = round(w / want)
            top = (h - new_h) // 2
            image = image.crop((0, top, w, top + new_h))
    return image


def status(asset) -> str:
    src = find_source(asset["id"])
    if src is None:
        return "missing"
    image = Image.open(src)
    problems = []
    w, h = image.size
    tw, th = asset["size"]
    if w < tw * 0.5 or h < th * 0.5:
        problems.append(f"small ({w}x{h}, want about {tw}x{th})")
    if abs((w / h) - (tw / th)) > 0.05:
        problems.append(f"aspect {w}/{h} differs from {tw}/{th} (will be cropped)")
    return "ok" if not problems else "check: " + "; ".join(problems)


def cmd_check():
    present = 0
    for asset in MANIFEST["assets"]:
        s = status(asset)
        present += s != "missing"
        print(f"{asset['id']:<22} {asset['source']:<11} {s}")
    print(f"\n{present}/{len(MANIFEST['assets'])} present")


GEMINI_RATIOS = {"1:1": 1.0, "3:4": 3 / 4, "4:3": 4 / 3, "9:16": 9 / 16, "16:9": 16 / 9}


def nearest_ratio(size) -> str:
    target = size[0] / size[1]
    return min(GEMINI_RATIOS, key=lambda name: abs(GEMINI_RATIOS[name] - target))


def cmd_prompts():
    lines = [
        "# 素材提示詞（給 Gemini 等圖像 AI 使用）", "",
        "## 怎麼用", "",
        "1. **先做 3 張試風格**：`compass_case`、`compass_face`、`traveller_fox`。風格滿意後，再把這張當「風格參考圖」一起上傳，生成其他素材，才會一致。",
        "2. **每張單獨生成**：把該素材的整段提示詞貼進去。提示詞開頭已經寫了比例；Gemini 支援的比例是 1:1、3:4、4:3、9:16、16:9，**匯出時會自動裁成規格尺寸**。",
        "3. **一律要純白底**（提示詞已寫）。Gemini 不會輸出透明背景，匯出工具會把與邊緣相連的白底去掉。",
        "4. **不要有文字**：地名、數字都由 App 畫。圖裡如果出現字，請重生成。",
        "5. **存檔**：存成 `android/assets/generated/<id>.png`（檔名就是下面標題裡的 id）。完成後執行 `python3 tools/build_assets.py check` 檢查、`export` 匯出；或把圖直接傳給 Claude 處理。",
        "6. 不滿意就用同一個提示詞再生成，或在對話裡說明要改什麼（例如「線條再細一點」「顏色更淡」）。", "",
        "## 共同風格（已經包含在每個提示詞裡，不用另外貼）", "", MANIFEST["baseStyle"], "",
        "## 負面提示詞", "",
        "Gemini 沒有獨立的負面提示詞欄位；提示詞裡已用「no text、no watermark…」表達。其他工具（Midjourney `--no`、Stable Diffusion）可用：", "",
        MANIFEST["negativePrompt"], "",
    ]
    for category in dict.fromkeys(a["category"] for a in MANIFEST["assets"]):
        lines += [f"## {category}", ""]
        for a in MANIFEST["assets"]:
            if a["category"] != category:
                continue
            w, h = a["size"]
            ratio = nearest_ratio(a["size"])
            lines += [f"### `{a['id']}` — {a['title']}", "",
                      f"- 用在：{a['usedIn']}", f"- 最終尺寸：{w}×{h}　Gemini 比例：{ratio}　背景：{'純白（匯出去背）' if a['alpha'] else '一般圖，不用去背'}"]
            if a["notes"]:
                lines.append(f"- 注意：{a['notes']}")
            lines.append("")
            if a["source"] == "procedural":
                lines += ["_由程式產生，不需要 AI 圖。_", ""]
            elif a.get("skipForAI"):
                lines += ["_建議跳過：由 Claude 以 SVG／程式繪製（可無縫平鋪）。_", ""]
            else:
                if a["source"] == "svg":
                    lines += ["_已有 SVG 手繪版可當備案；要用 Gemini 版的話用下面這段。_", ""]
                prompt = f"Create an image with aspect ratio {ratio}. " + MANIFEST["baseStyle"] + " " + a["prompt"]
                lines += ["```", prompt, "```", ""]
    (ROOT / "PROMPTS.md").write_text("\n".join(lines), encoding="utf-8")
    print("wrote PROMPTS.md")


def cmd_export():
    DRAWABLE.mkdir(parents=True, exist_ok=True)
    exported = []
    for asset in MANIFEST["assets"]:
        src = find_source(asset["id"])
        if src is None:
            continue
        image = Image.open(src)
        if asset["alpha"]:
            image = image.convert("RGBA") if image.mode == "RGBA" and image.getchannel("A").getextrema()[0] < 250 else knock_out_background(image, interior=asset.get("keyInterior", False))
        else:
            image = image.convert("RGB")
        image = fit(image, asset["size"])
        target = DRAWABLE / f"art_{asset['id']}.webp"
        image.save(target, "WEBP", quality=88, method=6, alpha_quality=100)
        exported.append(asset["id"])
        print(f"exported {asset['id']:<22} {image.size[0]}x{image.size[1]}  {target.stat().st_size // 1024} KB")
    write_launcher_icon(set(exported))
    print(f"\n{len(exported)} exported")


def write_launcher_icon(exported):
    """Uses the AI icon foreground when there is one, otherwise the built-in vector."""
    foreground = "@drawable/art_icon_foreground" if "icon_foreground" in exported else "@drawable/ic_launcher_foreground"
    background = "@drawable/art_icon_background" if "icon_background" in exported else "@color/ic_launcher_background"
    folder = RES / "mipmap-anydpi-v26"
    folder.mkdir(parents=True, exist_ok=True)
    xml = ('<?xml version="1.0" encoding="utf-8"?>\n'
           '<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">\n'
           f'    <background android:drawable="{background}" />\n'
           f'    <foreground android:drawable="{foreground}" />\n'
           '</adaptive-icon>\n')
    (folder / "ic_launcher.xml").write_text(xml, encoding="utf-8")
    (folder / "ic_launcher_round.xml").write_text(xml, encoding="utf-8")


def cmd_sheet():
    cell = 220
    items = [(a, find_source(a["id"])) for a in MANIFEST["assets"]]
    items = [(a, s) for a, s in items if s]
    cols = 6
    rows = max(1, (len(items) + cols - 1) // cols)
    sheet = Image.new("RGB", (cols * cell, rows * (cell + 22)), (241, 227, 190))
    draw = ImageDraw.Draw(sheet)
    for i, (asset, src) in enumerate(items):
        image = Image.open(src).convert("RGBA")
        image.thumbnail((cell - 12, cell - 12))
        x, y = (i % cols) * cell, (i // cols) * (cell + 22)
        sheet.paste(image, (x + 6, y + 6), image)
        draw.text((x + 6, y + cell), asset["id"], fill=(59, 42, 26))
    sheet.save(ROOT / "preview.png")
    print(f"wrote preview.png ({len(items)} assets)")


if __name__ == "__main__":
    commands = {"check": cmd_check, "prompts": cmd_prompts, "export": cmd_export, "sheet": cmd_sheet}
    if len(sys.argv) != 2 or sys.argv[1] not in commands:
        sys.exit(__doc__)
    commands[sys.argv[1]]()
