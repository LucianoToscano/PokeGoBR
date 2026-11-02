#!/usr/bin/env python3
"""PokeGoBR v0.0.8 - copy the fixed MediaProjection reader into the assembled app.
Apply only after scanner_v003 through scanner_v007.
"""
from pathlib import Path
import shutil

root = Path(__file__).resolve().parent.parent
source = root / "patches/LiveScannerService.kt"
target = root / "app/src/main/java/br/com/pokegobr/LiveScannerService.kt"
main = root / "app/src/main/java/br/com/pokegobr/MainActivity.kt"
gradle = root / "app/build.gradle.kts"

def one(s, before, after, label):
    n = s.count(before)
    if n != 1:
        raise RuntimeError(f"{label}: expected one match, found {n}")
    return s.replace(before, after, 1)

s = source.read_text(encoding="utf-8")
for required in [
    "override fun onCapturedContentResize",
    "override fun onCapturedContentVisibilityChanged",
    "wm.maximumWindowMetrics.bounds",
    "display.resize(width, height",
    "display.setSurface(reader.surface)",
    "if (paddedWidth == width) padded else",
    'Log.e("PokeGoBR-Scanner"',
    '"◉ Captura funcionando"',
]:
    if required not in s:
        raise RuntimeError(f"Missing MediaProjection safety fix: {required}")
if not target.is_file():
    raise RuntimeError("Missing extracted Android source")
shutil.copyfile(source, target)

activity = main.read_text(encoding="utf-8")
if "projectionManager.createScreenCaptureIntent()" not in activity:
    raise RuntimeError("Missing native Android user-choice capture intent")
activity = one(activity, "ALFA · 0.0.7", "ALFA · 0.0.8", "Scanner version")
activity = one(activity, "Sobre · v0.0.7", "Sobre · v0.0.8", "About version")
main.write_text(activity, encoding="utf-8")
config = gradle.read_text(encoding="utf-8")
config = one(config, "versionCode = 7", "versionCode = 8", "version code")
config = one(config, 'versionName = "0.0.7"', 'versionName = "0.0.8"', "version name")
gradle.write_text(config, encoding="utf-8")
print("0.0.8: live capture diagnostics, app-surface resizing and bitmap lifetime fix applied")
