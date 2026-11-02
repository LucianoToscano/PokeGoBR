#!/usr/bin/env python3
"""PokeGoBR 0.0.15: importação offline Poke Genie/Calcy/PGSStats.

Apply after scanner_v014.py. Never put user's actual exports in public sources.
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
        raise RuntimeError(f"{label}: esperado um trecho, observado {count}")
    return text.replace(before, after, 1)

for source, destination in (
    ("HubImportV15.kt", target / "HubImportV15.kt"),
    ("UnifiedHubV15.kt", target / "UnifiedHub.kt"),
    ("HubImportV15Test.kt", root / "app/src/test/java/br/com/pokegobr/HubImportV15Test.kt"),
):
    path = root / "patches" / source
    if not path.exists():
        raise RuntimeError(f"Fonte ausente: {path}")
    destination.parent.mkdir(parents=True, exist_ok=True)
    copyfile(path, destination)

src = activity.read_text(encoding="utf-8")
src = one(src, "ALFA · 0.0.14", "ALFA · 0.0.15", "versão na home")
src = one(src, "Sobre · v0.0.14", "Sobre · v0.0.15", "versão no menu")
activity.write_text(src, encoding="utf-8")

src = gradle.read_text(encoding="utf-8")
src = one(src, "versionCode = 14", "versionCode = 15", "versionCode")
src = one(src, 'versionName = "0.0.14"', 'versionName = "0.0.15"', "versionName")
gradle.write_text(src, encoding="utf-8")

hub = (target / "UnifiedHub.kt").read_text(encoding="utf-8")
for needle in ("HubImportV15.readAll(context, uri)", "Importar CSV/ZIP/JSON",
               "HubImportV15.previous(context, source)"):
    if needle not in hub:
        raise RuntimeError(f"Tela de importação sem integração: {needle}")

parser = (target / "HubImportV15.kt").read_text(encoding="utf-8")
for needle in ("ZipInputStream", "CSV incompatível", "PGSStats.json",
               "serialização Java", "Class # (G)", "GL Rank (min)"):
    if needle not in parser:
        raise RuntimeError(f"Importador incompleto: {needle}")

print("PokeGoBR 0.0.15: CSV Poke Genie e Calcy + ZIP/JSON PGSStats, importações por origem, PvP histórico e testes")
