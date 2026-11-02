#!/usr/bin/env python3
"""PokeGoBR 0.0.14: scanner visual compacto e OCR conservador.

After scanner_v013.py. Preserve prior modules, service capture and test suite.
"""
from pathlib import Path
from shutil import copyfile

root = Path(__file__).resolve().parent.parent
target = root / "app/src/main/java/br/com/pokegobr"
activity = target / "MainActivity.kt"
gradle = root / "app/build.gradle.kts"

def exactly_once(text: str, before: str, after: str, description: str) -> str:
    count = text.count(before)
    if count != 1:
        raise RuntimeError(f"{description}: esperada 1 ocorrência, obtidas {count}")
    return text.replace(before, after, 1)

for source, destination in [
    ("LiveScannerServiceV14.kt", target / "LiveScannerService.kt"),
    ("ScannerReadoutParser.kt", target / "ScannerReadoutParser.kt"),
    ("ScannerReadoutParserTest.kt",
     root / "app/src/test/java/br/com/pokegobr/ScannerReadoutParserTest.kt"),
    ("UnifiedHub.kt", target / "UnifiedHub.kt"),
]:
    src = root / "patches" / source
    if not src.is_file():
        raise RuntimeError(f"Patch ausente: {src}")
    destination.parent.mkdir(parents=True, exist_ok=True)
    copyfile(src, destination)

service = (target / "LiveScannerService.kt").read_text(encoding="utf-8")
for required in (
    "ScannerReadoutParser.parse(detected.text, speciesNames)",
    "visibility = View.GONE",
    "scanGate.setAutomatic(false)",
    "RaidScreenParser.parse(detected.text)",
    "scanGate.shouldProcess(SystemClock.elapsedRealtime())",
    "ScreenContextClassifier.classify(",
):
    if required not in service:
        raise RuntimeError(f"Scanner incompleto: {required}")
if "FLAG_SECURE" in service:
    raise RuntimeError("Regressão de screenshots: FLAG_SECURE não permitido")

src = activity.read_text(encoding="utf-8")
src = exactly_once(src, "ALFA · 0.0.13", "ALFA · 0.0.14", "versão na home")
src = exactly_once(src, "Sobre · v0.0.13", "Sobre · v0.0.14", "versão sobre")
activity.write_text(src, encoding="utf-8")

src = gradle.read_text(encoding="utf-8")
src = exactly_once(src, "versionCode = 13", "versionCode = 14", "versionCode")
src = exactly_once(src, 'versionName = "0.0.13"', 'versionName = "0.0.14"', "versionName")
gradle.write_text(src, encoding="utf-8")

hub = (target / "UnifiedHub.kt").read_text(encoding="utf-8")
if 'DexSpecies(656, "Froakie")' not in hub:
    raise RuntimeError("Froakie não está no índice inicial offline")
print("0.0.14: mini-bolha por padrão, PC robusto, nome por catálogo, IV estimado compacto e testes")
