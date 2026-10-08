#!/usr/bin/env python3
"""Build a street-level video along a route from Mapillary imagery.

Usage:
  export MAPILLARY_TOKEN='MLY|...'
  python route_video.py --start 25.0330,121.5654 --end 25.0478,121.5170 -o out.mp4

Personal, non-commercial use. Mapillary imagery is CC-BY-SA: credit Mapillary
contributors if you share the result.
"""
import argparse
import math
import os
import shutil
import subprocess
import sys
import time
from pathlib import Path

import requests

OSRM = "https://router.project-osrm.org/route/v1/driving"
GRAPH = "https://graph.mapillary.com/images"


def parse_pt(s):
    lat, lon = (float(x) for x in s.split(","))
    return lat, lon


def haversine(a, b):
    r = 6371000.0
    p1, p2 = math.radians(a[0]), math.radians(b[0])
    dp, dl = p2 - p1, math.radians(b[1] - a[1])
    h = math.sin(dp / 2) ** 2 + math.cos(p1) * math.cos(p2) * math.sin(dl / 2) ** 2
    return 2 * r * math.asin(math.sqrt(h))


def bearing(a, b):
    p1, p2 = math.radians(a[0]), math.radians(b[0])
    dl = math.radians(b[1] - a[1])
    y = math.sin(dl) * math.cos(p2)
    x = math.cos(p1) * math.sin(p2) - math.sin(p1) * math.cos(p2) * math.cos(dl)
    return (math.degrees(math.atan2(y, x)) + 360) % 360


def angle_diff(a, b):
    d = abs(a - b) % 360
    return min(d, 360 - d)


def get_route(start, end):
    url = f"{OSRM}/{start[1]},{start[0]};{end[1]},{end[0]}"
    r = requests.get(url, params={"overview": "full", "geometries": "geojson"}, timeout=30)
    r.raise_for_status()
    data = r.json()
    if data.get("code") != "Ok":
        sys.exit(f"OSRM error: {data.get('code')}")
    return [(lat, lon) for lon, lat in data["routes"][0]["geometry"]["coordinates"]]


def resample(path, step):
    """Points every `step` meters along the polyline, each with a heading."""
    out, carry = [], 0.0
    out.append((path[0], bearing(path[0], path[1])))
    for a, b in zip(path, path[1:]):
        seg = haversine(a, b)
        if seg == 0:
            continue
        hd = bearing(a, b)
        d = step - carry
        while d <= seg:
            t = d / seg
            out.append(((a[0] + (b[0] - a[0]) * t, a[1] + (b[1] - a[1]) * t), hd))
            d += step
        carry = seg - (d - step)
    return out


def nearby_images(token, pt, radius):
    dlat = radius / 111320.0
    dlon = radius / (111320.0 * math.cos(math.radians(pt[0])))
    bbox = f"{pt[1]-dlon},{pt[0]-dlat},{pt[1]+dlon},{pt[0]+dlat}"
    params = {
        "access_token": token,
        "fields": "id,computed_geometry,computed_compass_angle,thumb_2048_url,is_pano",
        "bbox": bbox,
        "limit": 100,
    }
    for attempt in range(4):
        r = requests.get(GRAPH, params=params, timeout=30)
        if r.status_code == 200:
            return r.json().get("data", [])
        if r.status_code in (429, 500, 502, 503):
            time.sleep(2 ** attempt)
            continue
        sys.exit(f"Mapillary API error {r.status_code}: {r.text[:200]}")
    return []


def pick_best(images, pt, heading, max_angle, allow_pano):
    best, best_score = None, None
    for im in images:
        if im.get("is_pano") and not allow_pano:
            continue
        geom, ang = im.get("computed_geometry"), im.get("computed_compass_angle")
        if not geom or ang is None or not im.get("thumb_2048_url"):
            continue
        lon, lat = geom["coordinates"]
        diff = angle_diff(ang, heading)
        if diff > max_angle:
            continue
        score = haversine(pt, (lat, lon)) + diff * 0.3  # distance + heading penalty
        if best_score is None or score < best_score:
            best, best_score = im, score
    return best


def download(url, dest):
    for attempt in range(4):
        try:
            r = requests.get(url, timeout=30)
            r.raise_for_status()
            dest.write_bytes(r.content)
            return True
        except requests.RequestException:
            time.sleep(2 ** attempt)
    return False


def main():
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("--start", required=True, help="lat,lon")
    ap.add_argument("--end", required=True, help="lat,lon")
    ap.add_argument("-o", "--output", default="route.mp4")
    ap.add_argument("--step", type=float, default=10, help="meters between frames (default 10)")
    ap.add_argument("--radius", type=float, default=25, help="search radius in meters (default 25)")
    ap.add_argument("--max-angle", type=float, default=60, help="max heading difference in degrees")
    ap.add_argument("--fps", type=int, default=12)
    ap.add_argument("--allow-pano", action="store_true", help="include 360 panoramas (look distorted)")
    ap.add_argument("--workdir", default="frames")
    ap.add_argument("--max-frames", type=int, default=0, help="cap frame count (0 = no cap)")
    ap.add_argument("--delay", type=float, default=0.1, help="seconds between API calls")
    args = ap.parse_args()

    token = os.environ.get("MAPILLARY_TOKEN")
    if not token:
        sys.exit("Set MAPILLARY_TOKEN first (export MAPILLARY_TOKEN='MLY|...').")
    if not shutil.which("ffmpeg"):
        sys.exit("ffmpeg not found in PATH.")

    start, end = parse_pt(args.start), parse_pt(args.end)
    print("Fetching route...")
    pts = resample(get_route(start, end), args.step)
    if args.max_frames:
        pts = pts[: args.max_frames]
    print(f"{len(pts)} sample points (every {args.step} m)")

    work = Path(args.workdir)
    work.mkdir(parents=True, exist_ok=True)
    for old in work.glob("*.jpg"):
        old.unlink()

    n, last_id, skipped = 0, None, 0
    for i, (pt, hd) in enumerate(pts):
        im = pick_best(nearby_images(token, pt, args.radius), pt, hd, args.max_angle, args.allow_pano)
        time.sleep(args.delay)
        if im is None:
            skipped += 1
            continue
        if im["id"] == last_id:  # avoid duplicate consecutive frames
            continue
        n += 1
        if not download(im["thumb_2048_url"], work / f"{n:05d}.jpg"):
            n -= 1
            skipped += 1
            continue
        last_id = im["id"]
        print(f"\r[{i+1}/{len(pts)}] frames={n} gaps={skipped}", end="", flush=True)
    print()

    if n < 2:
        sys.exit("Too few frames found. Try a bigger --radius / --max-angle, or a better-covered route.")

    cmd = [
        "ffmpeg", "-y", "-framerate", str(args.fps), "-i", str(work / "%05d.jpg"),
        "-vf", "scale=1280:720:force_original_aspect_ratio=decrease,pad=1280:720:(ow-iw)/2:(oh-ih)/2",
        "-c:v", "libx264", "-pix_fmt", "yuv420p", args.output,
    ]
    subprocess.run(cmd, check=True)
    print(f"Done: {args.output} ({n} frames, {skipped} points without imagery)")
    print("Imagery: Mapillary contributors, CC-BY-SA 4.0")


if __name__ == "__main__":
    main()
