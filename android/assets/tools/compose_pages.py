#!/usr/bin/env python3
"""Composes generated/paper_page.png (paper + handwritten notes) and the three review previews."""
from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parent.parent
GEN, PREVIEWS = ROOT / "generated", ROOT / "previews"


def main() -> None:
    PREVIEWS.mkdir(exist_ok=True)
    paper = Image.open(GEN / "paper_base.png").convert("RGBA")
    notes = Image.open(GEN / "paper_notes.png").convert("RGBA")
    page = Image.alpha_composite(paper, notes)
    page.convert("RGB").save(GEN / "paper_page.png", optimize=True)

    hero = Image.open(GEN / "hero_scene.png").convert("RGBA")
    crop = paper.crop((0, 260, 1080, 820)).copy()
    crop.alpha_composite(hero)

    face = Image.open(GEN / "compass_face.png").convert("RGBA").resize((590, 590))
    case = Image.open(GEN / "compass_case.png").convert("RGBA")
    compass = paper.crop((0, 500, 1024, 1620)).copy()
    compass.paste(face, (512 - 295, int(1120 * 0.57) - 295), face)
    compass.alpha_composite(case)

    page.convert("RGB").resize((540, 1080), Image.LANCZOS).save(PREVIEWS / "1_paper_page.jpg", quality=90)
    compass.convert("RGB").save(PREVIEWS / "2_compass.jpg", quality=92)
    crop.convert("RGB").save(PREVIEWS / "3_hero_scene.jpg", quality=92)
    print("wrote paper_page.png and previews/")


if __name__ == "__main__":
    main()
