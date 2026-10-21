#!/usr/bin/env python3
"""Builds the content pack that the app downloads: the three text files plus manifest.json.

    python3 content-tools/build_content.py <out_dir>

Run by CI after the tests pass (the tests check the same rules the app checks on arrival).
`published` is the build time in seconds; an app uses a pack only when it was published after
that app was built.
"""
import hashlib, json, pathlib, shutil, sys, time

SCHEMA = 1
FILES = ["card_texts.txt", "landmark_facts.txt", "town_blurbs.txt"]
RAW = pathlib.Path(__file__).resolve().parent.parent / "app/src/main/res/raw"

def main() -> None:
    out = pathlib.Path(sys.argv[1])
    out.mkdir(parents=True, exist_ok=True)
    files = {}
    for name in FILES:
        data = (RAW / name).read_bytes()
        shutil.copyfile(RAW / name, out / name)
        files[name] = {"sha256": hashlib.sha256(data).hexdigest(), "size": len(data)}
    manifest = {"schema": SCHEMA, "published": int(time.time()), "files": files}
    (out / "manifest.json").write_text(json.dumps(manifest, indent=2) + "\n", encoding="utf-8")
    print(f"wrote {out}/manifest.json for {len(files)} files")

if __name__ == "__main__":
    main()
