#!/usr/bin/env python3
"""PokeGoBR 0.0.12: OCR de raide com chefe, temporizador e participantes.
Executar após patches/scanner_v003.py .. scanner_v011.py.
"""
from pathlib import Path
from shutil import copyfile

root = Path(__file__).resolve().parent.parent
dst = root / "app/src/main/java/br/com/pokegobr"
main = dst / "MainActivity.kt"
gradle = root / "app/build.gradle.kts"

def once(source, before, after, label):
    matches = source.count(before)
    if matches != 1:
        raise RuntimeError(f"{label}: esperado 1 ocorrência, encontrado {matches}")
    return source.replace(before, after, 1)

for name, target in [
    ("RaidScreenParser.kt", dst / "RaidScreenParser.kt"),
    ("RaidScreenParserTest.kt",
     root / "app/src/test/java/br/com/pokegobr/RaidScreenParserTest.kt"),
    ("LiveScannerService.kt", dst / "LiveScannerService.kt"),
]:
    source = root / "patches" / name
    if not source.exists():
        raise RuntimeError(f"Fonte ausente: {source}")
    target.parent.mkdir(parents=True, exist_ok=True)
    copyfile(source, target)

service = (dst / "LiveScannerService.kt").read_text(encoding="utf-8")
if "RaidScreenParser.parse(detected.text)" not in service:
    raise RuntimeError("Scanner não está usando o OCR de raides")
if "ScreenContextClassifier.classify(" not in service or "FrameScanGate()" not in service:
    raise RuntimeError("Scanner perdeu o reconhecimento ou modos de captura anteriores")
if "FLAG_SECURE" in service:
    raise RuntimeError("Overlay voltou a bloquear prints")

kotlin = (dst / "RaidScreenParser.kt").read_text(encoding="utf-8")
if "recognized = true" not in kotlin or "val boss:" not in kotlin:
    raise RuntimeError("Raide OCR inválido")

activity = main.read_text(encoding="utf-8")
activity = once(activity, "ALFA · 0.0.11", "ALFA · 0.0.12", "scanner")
activity = once(activity, "Sobre · v0.0.11", "Sobre · v0.0.12", "sobre")
main.write_text(activity, encoding="utf-8")

g = gradle.read_text(encoding="utf-8")
g = once(g, "versionCode = 11", "versionCode = 12", "versionCode")
g = once(g, 'versionName = "0.0.11"', 'versionName = "0.0.12"', "versionName")
gradle.write_text(g, encoding="utf-8")
print("PokeGoBR 0.0.12: OCR conservador de raides e 9 testes Kotlin adicionados")
