#!/usr/bin/env python3
"""PokeGoBR 0.0.5: live MediaProjection scanner + overlay, after v003 and v004."""
from pathlib import Path
import shutil

root = Path(__file__).resolve().parent.parent

def once(s, old, new, label):
    n = s.count(old)
    if n != 1: raise RuntimeError(f"{label}: expected once; found {n}")
    return s.replace(old, new, 1)

main = root / "app/src/main/java/br/com/pokegobr/MainActivity.kt"
s = main.read_text(encoding="utf-8")
s = once(s, "import android.os.Bundle", """import android.os.Bundle
import android.os.Build
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.media.projection.MediaProjectionConfig
import android.media.projection.MediaProjectionManager""", "imports")
s = once(s, "    val recognizer = remember { TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS) }", """    val projectionManager = remember { context.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager }
    val projectionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK && result.data != null) {
            val launch = Intent(context, LiveScannerService::class.java).apply {
                action = LiveScannerService.START
                putExtra(LiveScannerService.EXTRA_CODE, result.resultCode)
                putExtra(LiveScannerService.EXTRA_DATA, result.data)
            }
            context.startForegroundService(launch)
        }
    }
    val recognizer = remember { TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS) }""", "launcher")
s = once(s, "Sobre · v0.0.4", "Sobre · v0.0.5", "about version")
s = once(s, "ALFA · 0.0.4", "ALFA · 0.0.5", "scanner version")
s = once(s, '"Selecione uma captura do Pokémon. O scanner tenta reconhecer PC e PS; confirme os valores para obter uma análise de IV confiável.",',
    '"Ative o scanner flutuante, autorize o Android e abra a avaliação no Pokémon GO. Não é necessário importar imagem.",', "intro")
s = once(s, """            Spacer(Modifier.height(12.dp))
            Button(onClick = { picker.launch("image/*") }, enabled = !scanning, modifier = Modifier.fillMaxWidth()) {""",
"""            Spacer(Modifier.height(12.dp))
            if (!raid) {
                Button(onClick = {
                    if (!Settings.canDrawOverlays(context)) {
                        errorText = "Autorize 'Aparecer sobre outros apps'. Depois volte e ative novamente."
                        context.startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            Uri.parse("package:" + context.packageName)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                    } else {
                        errorText = ""
                        val request = if (Build.VERSION.SDK_INT >= 34)
                            projectionManager.createScreenCaptureIntent(MediaProjectionConfig.createConfigForDefaultDisplay())
                        else projectionManager.createScreenCaptureIntent()
                        projectionLauncher.launch(request)
                    }
                }, modifier = Modifier.fillMaxWidth()) {
                    Text("◎  Ativar scanner flutuante")
                }
                Spacer(Modifier.height(4.dp))
                Text("Captura consentida e analisada localmente. Nenhuma imagem é salva ou enviada. Toque na bolha para ver o resultado e parar.", color = ice.copy(alpha = .83f), fontSize = 11.sp)
                Spacer(Modifier.height(8.dp))
            }
            OutlinedButton(onClick = { picker.launch("image/*") }, enabled = !scanning, modifier = Modifier.fillMaxWidth()) {""", "live button")
s = once(s, '"Lendo a imagem..." else "Selecionar captura da galeria"',
    '"Lendo a imagem..." else "Importar imagem (opcional)"', "gallery fallback")
main.write_text(s, encoding="utf-8")

manifest = root / "app/src/main/AndroidManifest.xml"
m = manifest.read_text(encoding="utf-8")
m = once(m, "<application android:allowBackup", """<uses-permission android:name="android.permission.SYSTEM_ALERT_WINDOW" />
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE_MEDIA_PROJECTION" />
    <application android:allowBackup""", "manifest permissions")
m = once(m, '        <activity android:name=".MainActivity"',
    '        <service android:name=".LiveScannerService" android:exported="false" android:foregroundServiceType="mediaProjection" />\n        <activity android:name=".MainActivity"', "manifest service")
manifest.write_text(m, encoding="utf-8")

detector = root / "app/src/main/java/br/com/pokegobr/AppraisalBarDetector.kt"
d = detector.read_text(encoding="utf-8")
d = once(d, "    private fun inspectBitmap(b: Bitmap)", "    fun inspectBitmap(b: Bitmap)", "detector visibility")
detector.write_text(d, encoding="utf-8")
src = root / "patches/LiveScannerService.kt"
if not src.exists(): raise RuntimeError("Missing live service source")
shutil.copyfile(src, root / "app/src/main/java/br/com/pokegobr/LiveScannerService.kt")

gradle = root / "app/build.gradle.kts"
g = gradle.read_text(encoding="utf-8")
g = once(g, "versionCode = 4", "versionCode = 5", "versionCode")
g = once(g, 'versionName = "0.0.4"', 'versionName = "0.0.5"', "versionName")
gradle.write_text(g, encoding="utf-8")
print("PokeGoBR 0.0.5 ready: live screen capture and OCR overlay")
