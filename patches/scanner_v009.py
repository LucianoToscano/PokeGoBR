#!/usr/bin/env python3
"""PokeGoBR 0.0.9 - remover janela segura do scanner para permitir prints.

Aplicar após scanner_v003.py .. scanner_v008.py. O patch v008 já copia
patches/LiveScannerService.kt, de onde FLAG_SECURE foi removido.
"""
from pathlib import Path

root = Path(__file__).resolve().parent.parent
main = root / "app/src/main/java/br/com/pokegobr/MainActivity.kt"
service = root / "app/src/main/java/br/com/pokegobr/LiveScannerService.kt"
gradle = root / "app/build.gradle.kts"

def one(source, before, after, label):
    count = source.count(before)
    if count != 1:
        raise RuntimeError(f"{label}: esperado um trecho; encontrado(s) {count}")
    return source.replace(before, after, 1)

service_code = service.read_text(encoding="utf-8")
if "WindowManager.LayoutParams.FLAG_SECURE" in service_code:
    raise RuntimeError("Scanner ainda impede capturas de tela com FLAG_SECURE")
if "WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN," not in service_code:
    raise RuntimeError("Flags esperadas da janela flutuante não foram encontradas")

# Prevent other screen-blocking flags in source; no OCR data is exported.
source_dir = root / "app/src/main"
for file in source_dir.rglob("*"):
    if file.is_file() and file.suffix in {".kt", ".java", ".xml"}:
        text = file.read_text(encoding="utf-8")
        if "FLAG_SECURE" in text or "android:windowSecure" in text:
            raise RuntimeError(f"Bloqueio de capturas ainda presente: {file}")

a = main.read_text(encoding="utf-8")
a = one(a, "ALFA · 0.0.8", "ALFA · 0.0.9", "versão do scanner")
a = one(a, "Sobre · v0.0.8", "Sobre · v0.0.9", "versão do menu")
main.write_text(a, encoding="utf-8")

g = gradle.read_text(encoding="utf-8")
g = one(g, "versionCode = 8", "versionCode = 9", "versionCode")
g = one(g, 'versionName = "0.0.8"', 'versionName = "0.0.9"', "versionName")
gradle.write_text(g, encoding="utf-8")

print("0.0.9: screenshot/gravacao de tela permitidos na bolha; sem FLAG_SECURE")
