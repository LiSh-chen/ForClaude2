#!/usr/bin/env bash
# Validate first, commit second. Refuses to commit when the township text has lint errors.
# Usage: android/tools/safe_commit.sh "message"
set -euo pipefail
cd "$(git rev-parse --show-toplevel)"
python3 android/tools/town_lint.py | tail -3
if ! python3 android/tools/town_lint.py >/dev/null; then echo "lint errors: not committing"; exit 1; fi
git add -A
git commit -q -m "$1" -m "Co-Authored-By: Claude Sonnet 5.5 <noreply@anthropic.com>
Claude-Session: https://claude.ai/code/session_01JdnZfnnjZFpnoB4FAXAtKB"
git push -q -u origin "$(git rev-parse --abbrev-ref HEAD)"
echo committed-and-pushed
