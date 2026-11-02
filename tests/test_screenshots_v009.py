"""Regressão: o scanner PokeGoBR não deve impedir prints ou gravadores de tela.

O teste valida arquivos Android resultantes da sequência de patches. Não substitui
teste em aparelho real, principalmente nas restrições de captura de outros apps.
"""
from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parent.parent
SOURCE = ROOT / "app/src/main"

class ScreenshotsAllowedTests(unittest.TestCase):
    def test_overlay_does_not_block_screenshot(self):
        content = (SOURCE / "java/br/com/pokegobr/LiveScannerService.kt").read_text(encoding="utf-8")
        self.assertNotIn("FLAG_SECURE", content)
        self.assertIn("WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY", content)
        self.assertIn("WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN", content)

    def test_no_screenshot_protection_in_android_sources(self):
        for path in SOURCE.rglob("*"):
            if path.is_file() and path.suffix in (".kt", ".java", ".xml"):
                content = path.read_text(encoding="utf-8")
                self.assertNotIn("FLAG_SECURE", content, str(path))
                self.assertNotIn("android:windowSecure", content, str(path))

    def test_capture_consent_still_used(self):
        content = (SOURCE / "java/br/com/pokegobr/MainActivity.kt").read_text(encoding="utf-8")
        self.assertIn("projectionManager.createScreenCaptureIntent()", content)

if __name__ == "__main__":
    unittest.main()
