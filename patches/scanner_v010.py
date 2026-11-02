#!/usr/bin/env python3
"""PokeGoBR 0.0.10 — classificador independente das telas do Pokémon GO.
Aplicar após patches/scanner_v003.py ... scanner_v009.py.
"""
from pathlib import Path
from shutil import copyfile

root = Path(__file__).resolve().parent.parent
main = root / "app/src/main/java/br/com/pokegobr/MainActivity.kt"
dst = root / "app/src/main/java/br/com/pokegobr"
gradle = root / "app/build.gradle.kts"

def once(source: str, old: str, new: str, label: str) -> str:
    n = source.count(old)
    if n != 1:
        raise RuntimeError(f"{label}: esperado 1 trecho, encontrado {n}")
    return source.replace(old, new, 1)

sources = [
    ("ScreenContextClassifier.kt", dst / "ScreenContextClassifier.kt"),
    ("LiveScannerService.kt", dst / "LiveScannerService.kt"),
    ("ScreenContextClassifierTest.kt",
     root / "app/src/test/java/br/com/pokegobr/ScreenContextClassifierTest.kt"),
]
for name, target in sources:
    source = root / "patches" / name
    if not source.is_file(): raise RuntimeError(f"Fonte ausente: {source}")
    target.parent.mkdir(parents=True, exist_ok=True)
    copyfile(source, target)

classifier = (dst / "ScreenContextClassifier.kt").read_text(encoding="utf-8")
service = (dst / "LiveScannerService.kt").read_text(encoding="utf-8")
for required in ("ScanScene.APPRAISAL", "ScanScene.DETAILS", "ScanScene.MOVES",
                 "ScanScene.RAID", "ScanScene.OTHER"):
    if required not in classifier: raise RuntimeError(f"Contexto ausente: {required}")
if "ScreenContextClassifier.classify(" not in service:
    raise RuntimeError("Leitor do overlay não usa o classificador")
if "FLAG_SECURE" in service:
    raise RuntimeError("Janela flutuante voltou a impedir prints")

activity = main.read_text(encoding="utf-8")
activity = once(activity, "ALFA · 0.0.9", "ALFA · 0.0.10", "scanner")
activity = once(activity, "Sobre · v0.0.9", "Sobre · v0.0.10", "sobre")
main.write_text(activity, encoding="utf-8")

g = gradle.read_text(encoding="utf-8")
g = once(g, "versionCode = 9", "versionCode = 10", "versionCode")
g = once(g, 'versionName = "0.0.9"', 'versionName = "0.0.10"', "versionName")
g = once(g, '    debugImplementation("androidx.compose.ui:ui-tooling")',
            '    testImplementation("junit:junit:4.13.2")\n    debugImplementation("androidx.compose.ui:ui-tooling")',
            "unit tests")
gradle.write_text(g, encoding="utf-8")
print("PokeGoBR 0.0.10: context classifier + Kotlin unit tests")
