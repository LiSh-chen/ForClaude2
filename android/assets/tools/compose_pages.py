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

    page.convert("RGB").resize((540, 1080), Image.LANCZOS).save(PREVIEWS / "1_paper_page.jpg", quality=90)
    print("wrote paper_page.png and previews/")


if __name__ == "__main__":
    main()
