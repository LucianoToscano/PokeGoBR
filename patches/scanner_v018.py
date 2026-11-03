#!/usr/bin/env python3
"""PokeGoBR 0.0.18: real Treecko screenshot regression:
- hide ALL app overlays before capturing a frame
- top-only CP OCR, name-plate species and name-line HP
- compact collapsed result, editable only after tapping the header
"""
from pathlib import Path
from shutil import copyfile

root = Path(__file__).resolve().parent.parent
dest = root / "app/src/main/java/br/com/pokegobr"
main = dest / "MainActivity.kt"
gradle = root / "app/build.gradle.kts"

def one(src, old, new, label):
    count = src.count(old)
    if count != 1:
        raise RuntimeError(f"{label}: expected one marker, got {count}")
    return src.replace(old, new, 1)

for source, destination in (
    ("PokemonHeaderParser.kt", dest / "PokemonHeaderParser.kt"),
    ("PokemonHeaderParserTest.kt",
     root / "app/src/test/java/br/com/pokegobr/PokemonHeaderParserTest.kt"),
    ("LiveScannerServiceV18.kt", dest / "LiveScannerService.kt"),
    ("LiveIvResultCardV18.kt", dest / "LiveIvResultCard.kt"),
    ("UnifiedHubV18.kt", dest / "UnifiedHub.kt"),
):
    file = root / "patches" / source
    if not file.is_file():
        raise RuntimeError(f"File absent: {file}")
    destination.parent.mkdir(parents=True, exist_ok=True)
    copyfile(file, destination)

src = main.read_text(encoding="utf-8")
src = one(src, "ALFA · 0.0.17", "ALFA · 0.0.18", "Home")
src = one(src, "Sobre · v0.0.17", "Sobre · v0.0.18", "About")
main.write_text(src, encoding="utf-8")

src = gradle.read_text(encoding="utf-8")
src = one(src, "versionCode = 17", "versionCode = 18", "versionCode")
src = one(src, 'versionName = "0.0.17"', 'versionName = "0.0.18"', "versionName")
gradle.write_text(src, encoding="utf-8")

service = (dest / "LiveScannerService.kt").read_text(encoding="utf-8")
for name in (
    "PokemonHeaderParser.parse(spatialLines, speciesNames)",
    "pendingCleanCapture = true",
    "overlay?.visibility = View.INVISIBLE",
    "frameAt - overlayHideRequestedAt < 350L",
    "overlay?.visibility = View.VISIBLE",
    "ivConsensus.observe(",
    "manualRequest = true",
    "resultCard?.show(result)",
    "RaidScreenParser.parse(detected.text)",
):
    if name not in service:
        raise RuntimeError(f"Scanner regressão: {name}")
if "val cp = readout.cp ?: fields.cp" in service:
    raise RuntimeError("Unsafe whole-screen CP fallback must not be reintroduced")

card = (dest / "LiveIvResultCard.kt").read_text(encoding="utf-8")
for name in (
    "expandedPanel = panel",
    "visibility = View.GONE",
    "collapseOnNew: Boolean = true",
    "result.iv.joinToString",
    "adjustIv(index: Int, delta: Int)",
):
    if name not in card:
        raise RuntimeError(f"Card regressão: {name}")

hub = (dest / "UnifiedHub.kt").read_text(encoding="utf-8")
if 'DexSpecies(252, "Treecko")' not in hub:
    raise RuntimeError("Treecko must be available without network")

print("0.0.18: clean frames, OCR anchored to top/nameplate, Treecko and compact tap-to-expand card")
