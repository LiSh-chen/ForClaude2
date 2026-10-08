# Street-level route video (Mapillary)

OSRM route -> sample every N m -> nearest Mapillary image facing the direction of travel -> ffmpeg mp4.

```bash
pip install -r requirements.txt
export MAPILLARY_TOKEN='MLY|...'   # never commit this
python route_video.py --start 25.0330,121.5654 --end 25.0478,121.5170 -o out.mp4 --max-frames 50
```

Tuning: `--step` (frame spacing), `--radius`/`--max-angle` (more matches, less accurate), `--fps`.
Imagery is CC-BY-SA 4.0 (Mapillary contributors); credit it if you share the video.
