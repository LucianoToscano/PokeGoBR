"""Offline regression tests using generated images only; do not commit user screenshots.
These tests validate the Python reference mirror; Android/Kotlin is compiled in Gradle.
"""
import tempfile
import unittest
from pathlib import Path
from PIL import Image, ImageDraw
from eval_v2 import detector


class AppraisalDetectorV2Test(unittest.TestCase):
    def setUp(self):
        self.tmp = tempfile.TemporaryDirectory()

    def tearDown(self):
        self.tmp.cleanup()

    def scenario(self, ivs=None, cover=False):
        picture = Image.new("RGB", (360, 1000), (24, 52, 70))
        canvas = ImageDraw.Draw(picture)
        if ivs is not None:
            for y, value in zip((610, 665, 720), ivs):
                canvas.rectangle((50, y, 230, y + 9), fill=(180, 180, 180))
                if value:
                    canvas.rectangle((50, y, 50 + round(181 * value / 15) - 1, y + 9), fill=(235, 120, 90))
            if cover:
                canvas.rectangle((48, 665, 150, 674), fill=(24, 52, 70))
        dest = str(Path(self.tmp.name) / "screenshot.png")
        picture.save(dest)
        return detector(dest)

    def test_mixed_ivs(self):
        self.assertEqual(self.scenario((1, 15, 14))[0], [1, 15, 14])

    def test_max_ivs(self):
        self.assertEqual(self.scenario((15, 15, 15))[0], [15, 15, 15])

    def test_min_ivs(self):
        self.assertEqual(self.scenario((0, 0, 0))[0], [0, 0, 0])

    def test_no_appraisal(self):
        self.assertIsNone(self.scenario())

    def test_occlusion_abstains(self):
        result = self.scenario((1, 15, 14), cover=True)
        self.assertTrue(result is None or not isinstance(result[0], list))


if __name__ == "__main__":
    unittest.main()
