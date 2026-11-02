package br.com.pokegobr

import android.app.*
import android.content.*
import android.content.pm.ServiceInfo
import android.graphics.*
import android.graphics.drawable.GradientDrawable
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.*
import android.provider.Settings
import android.view.*
import android.widget.*
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.max
import kotlin.math.roundToInt

/** Captura autorizada pelo Android. Analisa localmente, sem salvar ou enviar quadros. */
class LiveScannerService : Service() {
    companion object {
        const val START = "br.com.pokegobr.SCAN_START"
        const val STOP = "br.com.pokegobr.SCAN_STOP"
        const val EXTRA_CODE = "projection_code"
        const val EXTRA_DATA = "projection_data"
        private const val CHANNEL = "pokegobr_live_scanner"
        private const val NOTIF = 1005
    }
    private var projection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var imageReader: ImageReader? = null
    private var captureThread: HandlerThread? = null
    private var overlay: View? = null
    private var overlayParams: WindowManager.LayoutParams? = null
    private var manager: WindowManager? = null
    private var textView: TextView? = null
    private val busy = AtomicBoolean(false)
    private val main = Handler(Looper.getMainLooper())
    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    private var lastFrame = 0L
    private var stopped = false
    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == STOP) { stopSelf(); return START_NOT_STICKY }
        if (intent?.action != START || projection != null) return START_NOT_STICKY
        @Suppress("DEPRECATION")
        val data = if (Build.VERSION.SDK_INT >= 33)
            intent.getParcelableExtra(EXTRA_DATA, Intent::class.java)
        else intent.getParcelableExtra<Intent>(EXTRA_DATA)
        val code = intent.getIntExtra(EXTRA_CODE, Activity.RESULT_CANCELED)
        if (code != Activity.RESULT_OK || data == null) { stopSelf(); return START_NOT_STICKY }
        try {
            if (Build.VERSION.SDK_INT >= 26) {
                val channel = NotificationChannel(CHANNEL, "Scanner PokeGoBR", NotificationManager.IMPORTANCE_LOW)
                (getSystemService(NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(channel)
            }
            val stopIntent = Intent(this, LiveScannerService::class.java).setAction(STOP)
            val pending = PendingIntent.getService(this, 3, stopIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            val notification = Notification.Builder(this, CHANNEL)
                .setContentTitle("PokeGoBR — scanner de tela ativo")
                .setContentText("Análise local · toque para encerrar")
                .setSmallIcon(android.R.drawable.ic_menu_camera)
                .setOngoing(true).setContentIntent(pending).build()
            if (Build.VERSION.SDK_INT >= 29) startForeground(NOTIF, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION)
            else startForeground(NOTIF, notification)
            val projectionManager = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            projection = projectionManager.getMediaProjection(code, data) ?: error("Consentimento inválido")
            projection!!.registerCallback(object : MediaProjection.Callback() {
                override fun onStop() { main.post { stopSelf() } }
            }, main)
            startCapture()
            showBubble()
        } catch (_: Exception) { stopSelf() }
        return START_NOT_STICKY
    }

    private fun startCapture() {
        val dm = resources.displayMetrics
        val w = dm.widthPixels; val h = dm.heightPixels
        if (w <= 0 || h <= 0) error("Tamanho de tela inválido")
        val thread = HandlerThread("PokeGoBR-local-scanner")
        thread.start(); captureThread = thread
        val handler = Handler(thread.looper)
        val reader = ImageReader.newInstance(w, h, PixelFormat.RGBA_8888, 2)
        imageReader = reader
        reader.setOnImageAvailableListener({ source ->
            val image = runCatching { source.acquireLatestImage() }.getOrNull() ?: return@setOnImageAvailableListener
            try {
                if (System.currentTimeMillis() - lastFrame < 2000L || !busy.compareAndSet(false, true))
                    return@setOnImageAvailableListener
                lastFrame = System.currentTimeMillis()
                val plane = image.planes.firstOrNull()
                if (plane == null || plane.pixelStride != 4) { busy.set(false); return@setOnImageAvailableListener }
                val paddedWidth = plane.rowStride / plane.pixelStride
                if (paddedWidth < w) { busy.set(false); return@setOnImageAvailableListener }
                val padded = Bitmap.createBitmap(paddedWidth, h, Bitmap.Config.ARGB_8888)
                plane.buffer.rewind()
                padded.copyPixelsFromBuffer(plane.buffer)
                val cropped = Bitmap.createBitmap(padded, 0, 0, w, h)
                padded.recycle()
                val scale = minOf(1f, 1050f / max(w, h))
                val bitmap = if (scale < 1f) {
                    val resized = Bitmap.createScaledBitmap(cropped, (w * scale).roundToInt().coerceAtLeast(1), (h * scale).roundToInt().coerceAtLeast(1), true)
                    cropped.recycle(); resized
                } else cropped
                analyze(bitmap)
            } catch (_: Exception) {
                busy.set(false)
            } finally { image.close() }
        }, handler)
        virtualDisplay = projection!!.createVirtualDisplay(
            "PokeGoBR-visual", w, h, dm.densityDpi, DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            reader.surface, null, handler
        )
    }

    private fun analyze(bitmap: Bitmap) {
        val bars = runCatching { AppraisalBarDetector.inspectBitmap(bitmap) }.getOrDefault(AppraisalBarPreview())
        try {
            recognizer.process(InputImage.fromBitmap(bitmap, 0))
                .addOnSuccessListener { detected ->
                    val fields = ScreenshotParser.parse(detected.text)
                    val iv = bars.proposedIvs?.joinToString("/") ?: "indefinido"
                    val summary = "${fields.candidateName ?: "Pokémon"} · PC ${fields.cp ?: "—"} · PS ${fields.hp ?: "—"}\nIV A/D/PS: $iv ${if (bars.proposedIvs != null) "(estimativa)" else "(conferir)"}"
                    main.post { textView?.text = summary }
                }
                .addOnFailureListener { main.post { textView?.text = "Não foi possível ler esta tela." } }
                .addOnCompleteListener { bitmap.recycle(); busy.set(false) }
        } catch (_: Exception) { bitmap.recycle(); busy.set(false) }
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).roundToInt()

    private fun showBubble() {
        if (!Settings.canDrawOverlays(this)) return
        manager = getSystemService(WINDOW_SERVICE) as WindowManager
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = GradientDrawable().apply {
                setColor(Color.argb(238, 20, 32, 47)); cornerRadius = dp(14).toFloat()
                setStroke(dp(1), Color.argb(90, 240, 251, 255))
            }
        }
        val handle = TextView(this).apply {
            text = "◉  Scanner IV"; textSize = 13f; setTextColor(Color.WHITE)
            setPadding(dp(11), dp(9), dp(11), dp(9))
        }
        root.addView(handle)
        val details = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; visibility = View.GONE
            setPadding(dp(10), 0, dp(10), dp(9))
        }
        textView = TextView(this).apply {
            text = "Abra a avaliação do Pokémon.\nO resultado aparecerá aqui."
            textSize = 12f; setTextColor(Color.rgb(230, 249, 255))
        }
        details.addView(textView)
        val stop = TextView(this).apply {
            text = "■  Parar scanner"; textSize = 12f; setTextColor(Color.rgb(255, 180, 180))
            setPadding(0, dp(8), 0, 0)
            setOnClickListener { stopSelf() }
        }
        details.addView(stop); root.addView(details)
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or WindowManager.LayoutParams.FLAG_SECURE,
            PixelFormat.TRANSLUCENT
        ).apply { gravity = Gravity.TOP or Gravity.START; x = dp(12); y = dp(125) }
        overlayParams = params
        handle.setOnTouchListener(object : View.OnTouchListener {
            var downX = 0f; var downY = 0f; var x0 = 0; var y0 = 0; var moved = false
            override fun onTouch(v: View, event: MotionEvent): Boolean {
                when (event.actionMasked) {
                    MotionEvent.ACTION_DOWN -> {
                        downX = event.rawX; downY = event.rawY
                        x0 = params.x; y0 = params.y; moved = false
                    }
                    MotionEvent.ACTION_MOVE -> {
                        val dx = (event.rawX - downX).roundToInt()
                        val dy = (event.rawY - downY).roundToInt()
                        if (kotlin.math.abs(dx) > dp(7) || kotlin.math.abs(dy) > dp(7)) moved = true
                        if (moved) {
                            params.x = (x0 + dx).coerceAtLeast(0)
                            params.y = (y0 + dy).coerceAtLeast(0)
                            runCatching { manager?.updateViewLayout(root, params) }
                        }
                    }
                    MotionEvent.ACTION_UP -> if (!moved)
                        details.visibility = if (details.visibility == View.VISIBLE) View.GONE else View.VISIBLE
                }
                return true
            }
        })
        runCatching { manager?.addView(root, params); overlay = root }
    }

    override fun onDestroy() {
        if (stopped) { super.onDestroy(); return }
        stopped = true
        runCatching { overlay?.let { manager?.removeView(it) } }; overlay = null
        runCatching { imageReader?.setOnImageAvailableListener(null, null) }
        runCatching { virtualDisplay?.release() }; virtualDisplay = null
        runCatching { imageReader?.close() }; imageReader = null
        runCatching { projection?.stop() }; projection = null
        runCatching { captureThread?.quitSafely() }
        runCatching { recognizer.close() }
        super.onDestroy()
    }
}
