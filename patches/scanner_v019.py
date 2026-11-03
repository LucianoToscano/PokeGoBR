#!/usr/bin/env python3
"""PokeGoBR 0.0.19 — GoDex website integration (public links, no account sync).

Apply after scanner_v018.py. Public site is opened in the external browser
so login sessions and private collection tokens never enter PokeGoBR.
"""
from pathlib import Path
from shutil import copyfile

root = Path(__file__).resolve().parent.parent
dst = root / "app/src/main/java/br/com/pokegobr"
main = dst / "MainActivity.kt"
gradle = root / "app/build.gradle.kts"

def one(source, old, new, label):
    occurrences = source.count(old)
    if occurrences != 1:
        raise RuntimeError(f"{label}: expected one occurrence, found {occurrences}")
    return source.replace(old, new, 1)

for name, target in (
    ("GoDexLinks.kt", dst / "GoDexLinks.kt"),
    ("GoDexLinksTest.kt", root / "app/src/test/java/br/com/pokegobr/GoDexLinksTest.kt"),
    ("UnifiedHubV19.kt", dst / "UnifiedHub.kt"),
):
    file = root / "patches" / name
    if not file.is_file():
        raise RuntimeError(f"Missing v019 file: {file}")
    target.parent.mkdir(parents=True, exist_ok=True)
    copyfile(file, target)

screen = main.read_text(encoding="utf-8")
screen = one(screen, "ALFA · 0.0.18", "ALFA · 0.0.19", "Home version")
screen = one(screen, "Sobre · v0.0.18", "Sobre · v0.0.19", "About version")
main.write_text(screen, encoding="utf-8")

cfg = gradle.read_text(encoding="utf-8")
cfg = one(cfg, "versionCode = 18", "versionCode = 19", "Version code")
cfg = one(cfg, 'versionName = "0.0.18"', 'versionName = "0.0.19"', "Version name")
gradle.write_text(cfg, encoding="utf-8")

hub = (dst / "UnifiedHub.kt").read_text(encoding="utf-8")
for signature in (
    '"GoDex" -> HubGoDex()',
    'openGoDex(context, GoDexLinks.home)',
    'openGoDex(context, GoDexLinks.shinyDex)',
    'openGoDex(context, GoDexLinks.maxDex)',
    'GoDexLinks.validPublicCollection(collectionUrl)',
    'Intent(Intent.ACTION_VIEW, Uri.parse(url))',
    'HubImportV15.readAll(context, uri)',
):
    if signature not in hub:
        raise RuntimeError(f"Missing GoDex feature: {signature}")

sources = (dst / "GoDexLinks.kt").read_text(encoding="utf-8")
for url in (
    "https://godex.site/pt-BR",
    "https://godex.site/shinydex",
    "https://godex.site/maxdex",
):
    if url not in sources:
        raise RuntimeError(f"GoDex verified URL absent: {url}")
if "http://" in sources or "WebView(" in hub:
    raise RuntimeError("GoDex integration should use HTTPS and external browser")

print("PokeGoBR 0.0.19: GoDex collection entrypoints, ShinyDex, MaxDex, validated public links.")
