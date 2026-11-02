#!/usr/bin/env python3
"""PokeGoBR 0.0.11: modos Automático e Por toque no scanner flutuante.
Executar após patches/scanner_v003.py ... scanner_v010.py.
"""
from pathlib import Path
from shutil import copyfile

root = Path(__file__).resolve().parent.parent
main = root / "app/src/main/java/br/com/pokegobr/MainActivity.kt"
dest = root / "app/src/main/java/br/com/pokegobr"
gradle = root / "app/build.gradle.kts"

def one(source: str, old: str, new: str, label: str) -> str:
    found = source.count(old)
    if found != 1:
        raise RuntimeError(f"{label}: esperado 1 trecho; encontrados {found}")
    return source.replace(old, new, 1)

for name, target in [
    ("FrameScanGate.kt", dest / "FrameScanGate.kt"),
    ("LiveScannerService.kt", dest / "LiveScannerService.kt"),
    ("FrameScanGateTest.kt", root / "app/src/test/java/br/com/pokegobr/FrameScanGateTest.kt"),
]:
    source = root / "patches" / name
    if not source.is_file():
        raise RuntimeError(f"Fonte ausente: {source}")
    target.parent.mkdir(parents=True, exist_ok=True)
    copyfile(source, target)

service = (dest / "LiveScannerService.kt").read_text(encoding="utf-8")
for check in (
    "FrameScanGate()",
    "scanGate.shouldProcess(SystemClock.elapsedRealtime())",
    "scanGate.setAutomatic(false)",
    "scanGate.setAutomatic(true)",
    "scanGate.requestScan()",
    "Escanear agora",
    "ScreenContextClassifier.classify(",
):
    if check not in service:
        raise RuntimeError(f"Integração do scanner incompleta: {check}")
if "FLAG_SECURE" in service:
    raise RuntimeError("Proibição de prints voltou a aparecer")

activity = main.read_text(encoding="utf-8")
activity = one(activity, "ALFA · 0.0.10", "ALFA · 0.0.11", "versão scanner")
activity = one(activity, "Sobre · v0.0.10", "Sobre · v0.0.11", "versão menu")
main.write_text(activity, encoding="utf-8")

config = gradle.read_text(encoding="utf-8")
config = one(config, "versionCode = 10", "versionCode = 11", "versionCode")
config = one(config, 'versionName = "0.0.10"', 'versionName = "0.0.11"', "versionName")
gradle.write_text(config, encoding="utf-8")
print("0.0.11: modo automático/por toque e 7 testes JVM integrados")
