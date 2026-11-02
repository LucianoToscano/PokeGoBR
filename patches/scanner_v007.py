#!/usr/bin/env python3
"""PokeGoBR 0.0.7 - Restore the standard Android 14+ app-sharing chooser.

The 0.0.5 MediaProjection integration deliberately called
createConfigForDefaultDisplay(), which suppresses "Compartilhar um app".
This patch intentionally uses the standard no-argument consent intent so the
Android system, not our app, offers app-only or full-screen capture.
Apply only after scanner_v003.py through scanner_v006.py.
"""
from pathlib import Path

root = Path(__file__).resolve().parent.parent
main = root / "app/src/main/java/br/com/pokegobr/MainActivity.kt"
gradle = root / "app/build.gradle.kts"

def replace_once(text: str, before: str, after: str, label: str) -> str:
    count = text.count(before)
    if count != 1:
        raise RuntimeError(f"{label}: expected one occurrence, got {count}")
    return text.replace(before, after, 1)

source = main.read_text(encoding="utf-8")

source = replace_once(
    source,
    """val request = if (Build.VERSION.SDK_INT >= 34)
                            projectionManager.createScreenCaptureIntent(MediaProjectionConfig.createConfigForDefaultDisplay())
                        else projectionManager.createScreenCaptureIntent()""",
    """// Android 14+ oferece escolha entre um app e a tela inteira.
                        // Não usar createConfigForDefaultDisplay(): remove essa escolha.
                        val request = projectionManager.createScreenCaptureIntent()""",
    "restaurar seletor padrão de compartilhamento",
)
source = replace_once(
    source,
    "import android.media.projection.MediaProjectionConfig\n",
    "",
    "remover import de opção restritiva",
)
source = replace_once(
    source,
    '"Captura consentida e analisada localmente. Nenhuma imagem é salva ou enviada. Toque na bolha para ver o resultado e parar."',
    '"O Android permitirá escolher Pokémon GO (compartilhar um app) ou a tela inteira, quando disponível. Leitura local, sem salvar ou enviar imagens. Toque na bolha para parar."',
    "explicação de privacidade na interface",
)
source = replace_once(source, "ALFA · 0.0.6", "ALFA · 0.0.7", "versão do scanner")
source = replace_once(source, "Sobre · v0.0.6", "Sobre · v0.0.7", "versão sobre")
main.write_text(source, encoding="utf-8")

gradle_text = gradle.read_text(encoding="utf-8")
gradle_text = replace_once(gradle_text, "versionCode = 6", "versionCode = 7", "versionCode")
gradle_text = replace_once(gradle_text, 'versionName = "0.0.6"', 'versionName = "0.0.7"', "versionName")
gradle.write_text(gradle_text, encoding="utf-8")

assert "MediaProjectionConfig.createConfigForDefaultDisplay()" not in source.split("val request =")[1]
assert "val request = projectionManager.createScreenCaptureIntent()" in source
print("Scanner 0.0.7: seletor de app/tela inteira restaurado (quando suportado pelo Android)")
