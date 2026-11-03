#!/usr/bin/env python3
"""PokeGoBR v0.0.20: native scan history derived from provided Poke Genie UI.

Apply AFTER scanner_v019.py. Retains old scanner and imported data unchanged,
uses Android-private storage and opt-in image attachment.
"""
from pathlib import Path
from shutil import copyfile

root = Path(__file__).resolve().parent.parent
target = root / "app/src/main/java/br/com/pokegobr"
activity = target / "MainActivity.kt"
gradle = root / "app/build.gradle.kts"
hub = target / "UnifiedHub.kt"
service = target / "LiveScannerService.kt"

def once(content, old, new, name):
    n = content.count(old)
    if n != 1:
        raise RuntimeError(f"{name}: expected one anchor, found {n}")
    return content.replace(old, new, 1)

for source, destination in (
    ("ScanHistoryStoreV20.kt", target / "ScanHistoryStore.kt"),
    ("ScanPhotoStoreV20.kt", target / "ScanPhotoStore.kt"),
    ("ScanHistoryScreenV20.kt", target / "ScanHistoryScreen.kt"),
    ("ScanHistoryStoreTestV20.kt",
     root / "app/src/test/java/br/com/pokegobr/ScanHistoryStoreTest.kt"),
):
    path = root / "patches" / source
    if not path.is_file():
        raise RuntimeError(f"Missing history source: {path}")
    destination.parent.mkdir(parents=True, exist_ok=True)
    copyfile(path, destination)

src = hub.read_text(encoding="utf-8")
src = once(src,
    '''internal fun UnifiedModuleScreen(module: String, onNavigate: (Int) -> Unit, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth().verticalScroll(rememberScrollState())''',
    '''internal fun UnifiedModuleScreen(module: String, onNavigate: (Int) -> Unit, modifier: Modifier = Modifier) {
    if (module == "Meus Pokémon") {
        ScanHistoryScreen(onNavigate = onNavigate, modifier = modifier)
        return
    }
    Column(modifier.fillMaxWidth().verticalScroll(rememberScrollState())''',
    "history route")
hub.write_text(src, encoding="utf-8")

src = activity.read_text(encoding="utf-8")
src = once(src, "ALFA · 0.0.19", "ALFA · 0.0.20", "Home version")
src = once(src, "Sobre · v0.0.19", "Sobre · v0.0.20", "About version")
src = once(src,
    'DropdownMenuItem(text = { Text("Scanner de Pokémon") }, onClick = { selected = 8; showMenu = false })',
    'DropdownMenuItem(text = { Text("Scanner de Pokémon") }, onClick = { selected = 8; showMenu = false })\n'
    '                            DropdownMenuItem(text = { Text("Histórico de scans") }, onClick = { selected = 1; showMenu = false })',
    "hamburger history route")
activity.write_text(src, encoding="utf-8")

src = service.read_text(encoding="utf-8")
src = once(src,
    '''JSONObject()
                .put("name", result.species)''',
    '''JSONObject()
                .put("historyId", java.util.UUID.randomUUID().toString())
                .put("name", result.species)''',
    "new scan persistent ID")
service.write_text(src, encoding="utf-8")

src = gradle.read_text(encoding="utf-8")
src = once(src, "versionCode = 19", "versionCode = 20", "VersionCode")
src = once(src, 'versionName = "0.0.19"', 'versionName = "0.0.20"', "VersionName")
gradle.write_text(src, encoding="utf-8")

history = (target / "ScanHistoryScreen.kt").read_text(encoding="utf-8")
for required in (
    "ScanFilter.entries",
    "ScanSort.entries",
    "ScanHistoryStore.select(",
    "ScanPhotoStore.attach(",
    "ScanHistoryStore.copyImported(",
    "ScanHistoryStore.delete(",
    "ScanHistoryStore.exportCsv(",
    "ScanHistoryStore.update(",
):
    if required not in history:
        raise RuntimeError(f"History UI incomplete: {required}")
if "HubImportV15.readAll(context, uri)" not in history:
    raise RuntimeError("Legacy imports must remain available")
if "PokemonHeaderParser.parse" not in service.read_text(encoding="utf-8"):
    raise RuntimeError("Scanner v018 must remain intact")
if "GoDexLinks.home" not in hub.read_text(encoding="utf-8"):
    raise RuntimeError("GoDex module v019 must remain intact")
print("0.0.20: Poke Genie-style history, 7 filters, 7 sorts, details, review, CSV, opt-in photos.")
