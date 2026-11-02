#!/usr/bin/env python3
"""Apply AppraisalBarDetector V2 to the PokeGoBR 0.0.5 extracted source.
Must run after scanner_v003.py, scanner_v004.py and scanner_v005.py.
Fails closed on version/source drift instead of silently skipping changes.
"""
from pathlib import Path
import shutil

root = Path(__file__).resolve().parent.parent
source = root / "patches/AppraisalBarDetectorV2.kt"
target = root / "app/src/main/java/br/com/pokegobr/AppraisalBarDetector.kt"
main = root / "app/src/main/java/br/com/pokegobr/MainActivity.kt"
service = root / "app/src/main/java/br/com/pokegobr/LiveScannerService.kt"
gradle = root / "app/build.gradle.kts"

def replace_once(contents, old, new, label):
    count = contents.count(old)
    if count != 1:
        raise RuntimeError(f"{label}: expected one match, found {count}")
    return contents.replace(old, new, 1)

if not source.is_file() or not target.is_file() or not service.is_file():
    raise RuntimeError("Missing detector V2 or live scanner source")
v2 = source.read_text(encoding="utf-8")
assert "fun inspectBitmap(bitmap: Bitmap)" in v2
assert "fun inspect(context: Context, uri: Uri)" in v2
assert "internal object AppraisalBarDetector" in v2
shutil.copyfile(source, target)

screen = main.read_text(encoding="utf-8")
screen = replace_once(screen, "ALFA · 0.0.5", "ALFA · 0.0.6", "scanner version")
screen = replace_once(screen, "Sobre · v0.0.5", "Sobre · v0.0.6", "about version")
main.write_text(screen, encoding="utf-8")
g = gradle.read_text(encoding="utf-8")
g = replace_once(g, "versionCode = 5", "versionCode = 6", "versionCode")
g = replace_once(g, 'versionName = "0.0.5"', 'versionName = "0.0.6"', "versionName")
gradle.write_text(g, encoding="utf-8")
print("0.0.6: detector visual V2 applied to gallery and live MediaProjection scanner")
