"""Static regression checks for Android's user-consented MediaProjection integration.
These complement, but do not replace, device / instrumentation tests.
"""
from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parent.parent
ACTIVITY = ROOT / "app/src/main/java/br/com/pokegobr/MainActivity.kt"
SERVICE = ROOT / "app/src/main/java/br/com/pokegobr/LiveScannerService.kt"
MANIFEST = ROOT / "app/src/main/AndroidManifest.xml"

class ScreenCaptureRegressionTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.activity = ACTIVITY.read_text(encoding="utf-8")
        cls.service = SERVICE.read_text(encoding="utf-8")
        cls.manifest = MANIFEST.read_text(encoding="utf-8")

    def test_user_can_choose_app_or_full_screen(self):
        self.assertIn("projectionManager.createScreenCaptureIntent()", self.activity)
        self.assertNotIn("MediaProjectionConfig.createConfigForDefaultDisplay()", self.activity)

    def test_projection_service_is_private(self):
        self.assertIn('android:foregroundServiceType="mediaProjection"', self.manifest)
        self.assertIn('android:exported="false"', self.manifest)
        self.assertIn("FOREGROUND_SERVICE_MEDIA_PROJECTION", self.manifest)

    def test_capture_resize_does_not_reuse_consent(self):
        self.assertIn("override fun onCapturedContentResize", self.service)
        self.assertIn("display.resize(width, height", self.service)
        self.assertIn("display.setSurface(reader.surface)", self.service)
        self.assertEqual(self.service.count("createVirtualDisplay("), 1)

    def test_capture_status_and_errors_are_visible(self):
        self.assertIn("◉ Captura funcionando", self.service)
        self.assertIn('Log.e("PokeGoBR-Scanner"', self.service)
        self.assertIn("Toast.makeText", self.service)
        self.assertIn("visibility = View.VISIBLE", self.service)

    def test_no_premature_bitmap_recycle(self):
        self.assertIn("if (paddedWidth == width) padded else", self.service)
        self.assertIn("if (cropped !== padded) padded.recycle()", self.service)

if __name__ == "__main__":
    unittest.main()
