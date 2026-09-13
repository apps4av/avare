#!/usr/bin/env bash
# Write store/whatsnew/whatsnew-en-US from the latest Avare Releases entry in help.html.
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
HELP="$ROOT/app/src/main/assets/help.html"
OUT_DIR="$ROOT/store/whatsnew"
OUT="$OUT_DIR/whatsnew-en-US"

python3 - "$HELP" "$OUT" <<'PY'
import html, pathlib, re, sys

help_path, out_path = pathlib.Path(sys.argv[1]), pathlib.Path(sys.argv[2])
text = help_path.read_text(encoding="utf-8")
match = re.search(
    r'<h3 class="western">Avare Releases</h3>\s*'
    r'<p><b>([^<]+)</b></p>\s*'
    r'<ul>(.*?)</ul>',
    text,
    re.S,
)
if not match:
    sys.exit("Could not find the latest Avare Releases entry in help.html")

items = []
for raw in re.findall(r'<li>(.*?)</li>', match.group(2), re.S):
    line = html.unescape(re.sub(r'<[^>]+>', '', raw))
    line = ' '.join(line.split()).strip()
    if line:
        items.append(line)

footer = "Thank you for flying with Avare. Send feedback at the Apps4Av forum."
lines = items if items else ["Bug fixes."]
body = "\n".join(lines)
note = f"{body}\n{footer}" if len(body) + 1 + len(footer) <= 500 else body
if len(note) > 500:
    kept, used = [], 0
    for line in lines:
        extra = len(line) if not kept else len(line) + 1
        if used + extra > 500:
            break
        kept.append(line)
        used += extra
    note = "\n".join(kept) if kept else body[:500].rstrip()

out_path.parent.mkdir(parents=True, exist_ok=True)
out_path.write_text(note.rstrip() + "\n", encoding="utf-8")
print(f"Wrote {out_path} ({len(note)} chars) from Avare {match.group(1).strip()}")
PY
