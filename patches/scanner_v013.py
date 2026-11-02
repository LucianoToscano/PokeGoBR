#!/usr/bin/env python3
"""PokeGoBR v0.0.13 — central modular usando fontes próprias e índice sanitizado.

Execute somente após scanner_v003.py a scanner_v012.py. O ZIP base permanece
imutável e o GitHub Actions gera um fonte editável consolidado do APK resultante.
"""
from pathlib import Path
from shutil import copyfile

root = Path(__file__).resolve().parent.parent
dst = root / "app/src/main/java/br/com/pokegobr"
main = dst / "MainActivity.kt"
manifest = root / "app/src/main/AndroidManifest.xml"
gradle = root / "app/build.gradle.kts"

def once(value, old, new, label):
    n = value.count(old)
    if n != 1:
        raise RuntimeError(f"{label}: esperado exatamente 1 ocorrência, encontrado {n}")
    return value.replace(old, new, 1)

for source_name, target in (
    ("UnifiedHub.kt", dst / "UnifiedHub.kt"),
    ("HubCollectionImporter.kt", dst / "HubCollectionImporter.kt"),
    ("GeoCoordinateParser.kt", dst / "GeoCoordinateParser.kt"),
    ("UnifiedHubTest.kt", root / "app/src/test/java/br/com/pokegobr/UnifiedHubTest.kt"),
    ("pokegobr_reference_catalog.json", root / "app/src/main/assets/pokegobr_reference_catalog.json"),
):
    source = root / "patches" / source_name
    if not source.is_file():
        raise RuntimeError(f"Arquivo necessário ausente: {source}")
    target.parent.mkdir(parents=True, exist_ok=True)
    copyfile(source, target)

content = main.read_text(encoding="utf-8")
content = once(
    content,
    'AppModule("Meus Pokémon", R.drawable.menu_pokemon, Screen.SCANNER_POKEMON)',
    'AppModule("Meus Pokémon", R.drawable.menu_pokemon, Screen.OTHER)',
    "rota da coleção"
)
start = content.find('                    Screen.OTHER -> Box(')
if start < 0:
    raise RuntimeError("Não foi encontrado bloco de módulos ainda não implementados")
# Locate the precise Compose block (old placeholder) bounded by the two case closures.
old = '''                    Screen.OTHER -> Box(Modifier.weight(1f).padding(top = 12.dp), contentAlignment = Alignment.TopCenter) {
                        FrostedPanel(Modifier.fillMaxWidth(.90f)) {
                            Text(selectedModule.title, color = ice, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
                            Spacer(Modifier.height(8.dp))
                            Text("Módulo previsto no Arquivo Mestre PokeGoBR. Nesta versão, o Scanner de Pokémon e Raides já permite analisar capturas de tela.", color = ice.copy(alpha = .85f), fontSize = 13.sp)
                            Spacer(Modifier.height(12.dp))
                            OutlinedButton(onClick = { selected = 8 }) { Text("Abrir Scanner") }
                        }
                    }'''
new = '''                    Screen.OTHER -> UnifiedModuleScreen(
                        module = selectedModule.title,
                        onNavigate = { selected = it },
                        modifier = Modifier.weight(1f)
                    )'''
content = once(content, old, new, "substituição do placeholder por páginas funcionais")
content = once(content, "ALFA · 0.0.12", "ALFA · 0.0.13", "versão do scanner")
content = once(content, "Sobre · v0.0.12", "Sobre · v0.0.13", "versão do menu")
main.write_text(content, encoding="utf-8")

xml = manifest.read_text(encoding="utf-8")
if 'android.permission.INTERNET' not in xml:
    xml = once(
        xml,
        '    <uses-permission android:name="android.permission.SYSTEM_ALERT_WINDOW" />',
        '    <uses-permission android:name="android.permission.INTERNET" />\n' +
        '    <uses-permission android:name="android.permission.SYSTEM_ALERT_WINDOW" />',
        "permissão para catálogo público sob pedido do usuário"
    )
manifest.write_text(xml, encoding="utf-8")

config = gradle.read_text(encoding="utf-8")
config = once(config, "versionCode = 12", "versionCode = 13", "versionCode")
config = once(config, 'versionName = "0.0.12"', 'versionName = "0.0.13"', "versionName")
config = once(config, '    testImplementation("junit:junit:4.13.2")',
              '    testImplementation("junit:junit:4.13.2")\n' +
              '    testImplementation("org.json:json:20240303")',
              "testes JVM JSON local")
gradle.write_text(config, encoding="utf-8")

service = (dst / "LiveScannerService.kt").read_text(encoding="utf-8")
for feature in ("ScreenContextClassifier.classify(", "FrameScanGate()",
                "RaidScreenParser.parse(detected.text)"):
    if feature not in service:
        raise RuntimeError(f"Regressão do scanner anterior: {feature}")
if "FLAG_SECURE" in service:
    raise RuntimeError("Regressão de captura de screenshot")

import json
reference_catalog = json.loads(
    (root / "app/src/main/assets/pokegobr_reference_catalog.json").read_text(encoding="utf-8")
)
if len(reference_catalog["records"]) != 54:
    raise RuntimeError("Inventário de referências deve conter 54 entradas")
if any("drive.google.com" in str(x) for x in reference_catalog["records"]):
    raise RuntimeError("O índice público não pode incluir links privados do Google Drive")

print("v0.0.13: Pokédex taxonômica, coleção local, IV, PvP orientativo, notas de trocas/itens, comunidade, coordenadas, GO Rocket e inventário sanitizado")
