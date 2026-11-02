#!/usr/bin/env python3
"""PokeGoBR 0.0.16: dedicated IV result card and optional historical PvP ranks.

Apply after scanner_v015.py, preserving every prior build step and feature.
"""
from pathlib import Path
from shutil import copyfile

root = Path(__file__).resolve().parent.parent
target = root / "app/src/main/java/br/com/pokegobr"
activity = target / "MainActivity.kt"
gradle = root / "app/build.gradle.kts"

def one(text, before, after, label):
    count = text.count(before)
    if count != 1:
        raise RuntimeError(f"{label}: expected one marker, found {count}")
    return text.replace(before, after, 1)

for name, destination in (
    ("LiveIvResultCard.kt", target / "LiveIvResultCard.kt"),
    ("LiveScannerServiceV16.kt", target / "LiveScannerService.kt"),
    ("LiveIvResultTest.kt", root / "app/src/test/java/br/com/pokegobr/LiveIvResultTest.kt"),
):
    source = root / "patches" / name
    if not source.is_file():
        raise RuntimeError(f"Missing v016 source: {source}")
    destination.parent.mkdir(parents=True, exist_ok=True)
    copyfile(source, destination)

content = activity.read_text(encoding="utf-8")
content = one(content, "ALFA · 0.0.15", "ALFA · 0.0.16", "Home version")
content = one(content, "Sobre · v0.0.15", "Sobre · v0.0.16", "About version")
activity.write_text(content, encoding="utf-8")

content = gradle.read_text(encoding="utf-8")
content = one(content, "versionCode = 15", "versionCode = 16", "Build code")
content = one(content, 'versionName = "0.0.15"', 'versionName = "0.0.16"', "Build name")
gradle.write_text(content, encoding="utf-8")

service = (target / "LiveScannerService.kt").read_text(encoding="utf-8")
for signature in (
    "resultCard?.show(result)",
    "saveEstimatedResult(result)",
    "findImportedRanks(safeName, iv)",
    "ScannerReadoutParser.parse(detected.text, speciesNames)",
    "scanGate.shouldProcess(SystemClock.elapsedRealtime())",
    "RaidScreenParser.parse(detected.text)",
    "ScreenContextClassifier.classify(",
):
    if signature not in service:
        raise RuntimeError(f"Missing card integration: {signature}")
if "FLAG_SECURE" in service:
    raise RuntimeError("Do not return the secure screenshot-blocking overlay flag")

card = (target / "LiveIvResultCard.kt").read_text(encoding="utf-8")
for signature in (
    "class LiveIvResultCard(",
    "private val onRescan",
    "private val onSave",
    "fun dispose()",
    "IV visual estimado",
):
    if signature not in card:
        raise RuntimeError(f"Missing visual IV card feature: {signature}")

print("PokeGoBR 0.0.16: IV card with species/PC/HP/%/ATK/DEF/STA, history, drag, rescan, archived PvP rank.")
