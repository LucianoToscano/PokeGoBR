#!/usr/bin/env python3
"""PokeGoBR 0.0.17: stabilize automatic IV readings and permit manual review.
Run after scanner_v016.py; historical patch files must remain unchanged.
"""
from pathlib import Path
from shutil import copyfile

root = Path(__file__).resolve().parent.parent
dest = root / "app/src/main/java/br/com/pokegobr"
main = dest / "MainActivity.kt"
gradle = root / "app/build.gradle.kts"

def one(text, before, after, name):
    occurrences = text.count(before)
    if occurrences != 1:
        raise RuntimeError(f"{name}: 1 occurrence expected, found {occurrences}")
    return text.replace(before, after, 1)

for src, dst in (
    ("IvScanConsensus.kt", dest / "IvScanConsensus.kt"),
    ("LiveScannerServiceV17.kt", dest / "LiveScannerService.kt"),
    ("LiveIvResultCardV17.kt", dest / "LiveIvResultCard.kt"),
    ("IvScanConsensusTest.kt", root / "app/src/test/java/br/com/pokegobr/IvScanConsensusTest.kt"),
):
    original = root / "patches" / src
    if not original.is_file():
        raise RuntimeError(f"File absent: {original}")
    dst.parent.mkdir(parents=True, exist_ok=True)
    copyfile(original, dst)

screen = main.read_text(encoding="utf-8")
screen = one(screen, "ALFA · 0.0.16", "ALFA · 0.0.17", "Home scanner version")
screen = one(screen, "Sobre · v0.0.16", "Sobre · v0.0.17", "About screen version")
main.write_text(screen, encoding="utf-8")

build = gradle.read_text(encoding="utf-8")
build = one(build, "versionCode = 16", "versionCode = 17", "Build code")
build = one(build, 'versionName = "0.0.16"', 'versionName = "0.0.17"', "Build name")
gradle.write_text(build, encoding="utf-8")

service = (dest / "LiveScannerService.kt").read_text(encoding="utf-8")
card = (dest / "LiveIvResultCard.kt").read_text(encoding="utf-8")
for required in (
    "SpatialSpeciesResolver.resolve(spatialLines, speciesNames)",
    "ivConsensus.observe(",
    "IvReadiness.Waiting",
    "result.userConfirmed",
    "manualRequest = true",
    "RaidScreenParser.parse(detected.text)",
    "scanGate.shouldProcess(SystemClock.elapsedRealtime())",
):
    if required not in service:
        raise RuntimeError(f"Missing IV scanner integration: {required}")

for required in (
    "private fun adjustIv(index: Int, delta: Int)",
    "Conferi os IVs na avaliação",
    "pvpFromImports = emptyList()",
    "userConfirmed = false",
):
    if required not in card:
        raise RuntimeError(f"Missing manual card feature: {required}")

if "FLAG_SECURE" in service or "FLAG_SECURE" in card:
    raise RuntimeError("Scanner must not re-enable screenshot blocking")

print("0.0.17: two-frame auto consensus, spatial name recognition, +/- editing and user-reviewed saving")
