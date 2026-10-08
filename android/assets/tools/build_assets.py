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


def cmd_prompts():
    lines = ["# 素材提示詞（可直接貼給圖像 AI）", "",
             "每個素材的完整提示詞 = 共同風格 + 該素材描述。生成後存成 `generated/<id>.png`，再執行 `export`。", "",
             "**共同風格**", "", MANIFEST["baseStyle"], "", "**負面提示詞（支援的工具才填）**", "", MANIFEST["negativePrompt"], ""]
    for category in dict.fromkeys(a["category"] for a in MANIFEST["assets"]):
        lines += [f"## {category}", ""]
        for a in MANIFEST["assets"]:
            if a["category"] != category:
                continue
            w, h = a["size"]
            lines += [f"### `{a['id']}` — {a['title']}", "",
                      f"- 用在：{a['usedIn']}", f"- 尺寸：{w}×{h}　透明背景：{'是（用純白底生成，匯出時去背）' if a['alpha'] else '否'}"]
            if a["notes"]:
                lines.append(f"- 注意：{a['notes']}")
            lines.append("")
            if a["source"] == "procedural":
                lines += ["_由程式產生，不需要 AI 圖。_", ""]
            else:
                lines += ["```", MANIFEST["baseStyle"] + " " + a["prompt"], "```", ""]
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
