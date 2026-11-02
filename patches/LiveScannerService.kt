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
import android.util.Log
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
    private var bubbleTitle: TextView? = null
    private var captureWorker: Handler? = null
    private var captureWidth = 0
    private var captureHeight = 0
    private var framesSeen = 0
    private val scanGate = FrameScanGate()
    private val busy = AtomicBoolean(false)
    private val main = Handler(Looper.getMainLooper())
    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    private val speciesNames by lazy { HubData.cachedSpecies(this).map { it.name } }
    private var resumeAutomaticAfterMenu = true
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
                override fun onCapturedContentResize(width: Int, height: Int) {
                    resizeCapture(width, height)
                }
                override fun onCapturedContentVisibilityChanged(isVisible: Boolean) {
                    if (!isVisible) updateStatus("◉ Scanner pausado", "Abra o app compartilhado para continuar a leitura.")
                    else updateStatus("◉ Scanner ativo", "Tela compartilhada visível. Abra a avaliação do Pokémon.")
                }
            }, main)
            showBubble()
            startCapture()
        } catch (error: Exception) {
            Log.e("PokeGoBR-Scanner", "Não foi possível iniciar a projeção", error)
            Toast.makeText(this, "Scanner: falha ao iniciar (" + error.javaClass.simpleName + ")", Toast.LENGTH_LONG).show()
            stopSelf()
        }
        return START_NOT_STICKY
    }

    private fun updateStatus(title: String, message: String? = null) {
        main.post {
            if (stopped) return@post
            bubbleTitle?.text = title
            if (message != null) textView?.text = message
        }
    }

    private fun initialSize(): Pair<Int, Int> {
        if (Build.VERSION.SDK_INT >= 30) {
            val wm = getSystemService(WINDOW_SERVICE) as WindowManager
            val bounds = wm.maximumWindowMetrics.bounds
            return Pair(bounds.width().coerceAtLeast(1), bounds.height().coerceAtLeast(1))
        }
        @Suppress("DEPRECATION")
        val metrics = android.util.DisplayMetrics()
        @Suppress("DEPRECATION")
        (getSystemService(WINDOW_SERVICE) as WindowManager).defaultDisplay.getRealMetrics(metrics)
        return Pair(metrics.widthPixels.coerceAtLeast(1), metrics.heightPixels.coerceAtLeast(1))
    }

    private fun startCapture() {
        val (width, height) = initialSize()
        if (width < 100 || height < 100) throw IllegalStateException("Resolução inválida")
        val thread = HandlerThread("PokeGoBR-local-scanner")
        thread.start()
        captureThread = thread
        val handler = Handler(thread.looper)
        captureWorker = handler
        val reader = makeReader(width, height, handler)
        imageReader = reader
        captureWidth = width
        captureHeight = height
        virtualDisplay = projection!!.createVirtualDisplay(
            "PokeGoBR-visual", width, height, resources.configuration.densityDpi,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            reader.surface, null, handler
        )
        updateStatus("◉ Captura iniciada", "Aguardando imagem. Abra a avaliação do Pokémon no app compartilhado.")
    }

    private fun makeReader(width: Int, height: Int, handler: Handler): ImageReader {
        val reader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2)
        reader.setOnImageAvailableListener({ source ->
            val image = runCatching { source.acquireLatestImage() }.getOrNull()
                ?: return@setOnImageAvailableListener
            try {
                if (stopped || !busy.compareAndSet(false, true)) return@setOnImageAvailableListener
                if (!scanGate.shouldProcess(SystemClock.elapsedRealtime())) {
                    busy.set(false)
                    return@setOnImageAvailableListener
                }
                lastFrame = System.currentTimeMillis()
                framesSeen++
                if (framesSeen == 1) {
                    updateStatus("◉ Captura funcionando", "Primeiro quadro recebido. Abra a avaliação do Pokémon.")
                }
                val plane = image.planes.firstOrNull()
                    ?: throw IllegalStateException("Quadro sem plano de pixels")
                if (plane.pixelStride != 4) throw IllegalStateException("Formato de pixel não suportado")
                val paddedWidth = plane.rowStride / plane.pixelStride
                if (paddedWidth < width) throw IllegalStateException("Stride inválido")
                val padded = Bitmap.createBitmap(paddedWidth, height, Bitmap.Config.ARGB_8888)
                plane.buffer.rewind()
                padded.copyPixelsFromBuffer(plane.buffer)
                // With zero padding the crop is the same Bitmap. Never recycle it prematurely.
                val cropped = if (paddedWidth == width) padded else
                    Bitmap.createBitmap(padded, 0, 0, width, height)
                if (cropped !== padded) padded.recycle()
                val scale = minOf(1f, 1250f / max(width, height))
                val bitmap = if (scale < 1f) {
                    val small = Bitmap.createScaledBitmap(
                        cropped, (width * scale).roundToInt().coerceAtLeast(1),
                        (height * scale).roundToInt().coerceAtLeast(1), true
                    )
                    if (small !== cropped) cropped.recycle()
                    small
                } else cropped
                analyze(bitmap)
            } catch (error: Exception) {
                busy.set(false)
                Log.e("PokeGoBR-Scanner", "Falha ao ler quadro", error)
                updateStatus("⚠ Erro no quadro", "Erro de leitura (" + error.javaClass.simpleName + "). Reinicie o scanner.")
            } finally {
                image.close()
            }
        }, handler)
        return reader
    }

    /** Resize one VirtualDisplay and its surface; do not consume the projection token twice. */
    private fun resizeCapture(width: Int, height: Int) {
        if (width < 100 || height < 100 || stopped) return
        captureWorker?.post {
            if (stopped || (width == captureWidth && height == captureHeight)) return@post
            val display = virtualDisplay ?: return@post
            try {
                val worker = captureWorker ?: return@post
                val reader = makeReader(width, height, worker)
                display.resize(width, height, resources.configuration.densityDpi)
                display.setSurface(reader.surface)
                val old = imageReader
                imageReader = reader
                captureWidth = width
                captureHeight = height
                old?.setOnImageAvailableListener(null, null)
                old?.close()
                updateStatus("◉ Área ajustada", "Capturando " + width + " × " + height + " pixels. Abra a avaliação.")
            } catch (error: Exception) {
                Log.e("PokeGoBR-Scanner", "Falha ao redimensionar", error)
                updateStatus("⚠ Área de captura", "Não foi possível ajustar. Pare e inicie uma nova captura.")
            }
        }
    }

    private fun analyze(bitmap: Bitmap) {
        val bars = runCatching { AppraisalBarDetector.inspectBitmap(bitmap) }
            .getOrDefault(AppraisalBarPreview())
        try {
            recognizer.process(InputImage.fromBitmap(bitmap, 0))
                .addOnSuccessListener { detected ->
                    val fields = ScreenshotParser.parse(detected.text)
                    // Validate names against the local species index. Never accept "PESO"
                    // or other menu labels as Pokémon names.
                    val readout = ScannerReadoutParser.parse(detected.text, speciesNames)
                    val cp = readout.cp ?: fields.cp
                    val hp = readout.hp ?: fields.hp
                    val safeName = readout.species
                    val scene = ScreenContextClassifier.classify(
                        detected.text, bars.barRowsFound, cp, hp
                    )
                    val brief = (safeName?.let { "$it · " } ?: "") +
                        "PC " + (cp?.toString() ?: "—") +
                        " · PS " + (hp?.toString() ?: "—")
                    when (scene.scene) {
                        ScanScene.APPRAISAL -> {
                            val iv = bars.proposedIvs
                            val compactIv = iv?.let { ScannerReadoutParser.tentativeIvLabel(it) }
                            if (compactIv != null) {
                                updateStatus("◉ " + compactIv,
                                    brief + "\nIV estimado: " + (iv?.joinToString("/") ?: "—") +
                                        "\nLeitura experimental. Confira as barras antes de salvar.")
                            } else {
                                updateStatus("◉ Avaliação · IV inconclusivo",
                                    brief + "\nAvaliação encontrada, mas barras insuficientes.\n" +
                                        "Não é possível confirmar IV.")
                            }
                        }
                        ScanScene.DETAILS -> {
                            updateStatus("◉ " + (safeName ?: "Pokémon") +
                                (cp?.let { " · PC $it" } ?: ""),
                                brief + "\nAbra Avaliar para conferir os IVs.")
                        }
                        ScanScene.MOVES -> {
                            updateStatus("◉ Golpes", "Leitura detalhada dos golpes ainda em desenvolvimento.")
                        }
                        ScanScene.RAID -> {
                            val raidInfo = RaidScreenParser.parse(detected.text)
                            updateStatus("◉ Raide", if (raidInfo.recognized)
                                raidInfo.display() else "Raide possivelmente detectada; dados inconclusivos.")
                        }
                        ScanScene.OTHER -> {
                            updateStatus("◉ Scanner ativo",
                                "Abra um Pokémon e toque em Avaliar. Nenhum IV foi confirmado.")
                        }
                    }
                }
                .addOnFailureListener {
                    updateStatus("⚠ OCR indisponível", "Não foi possível reconhecer texto nesta captura.")
                }
                .addOnCompleteListener { bitmap.recycle(); busy.set(false) }
        } catch (error: Exception) {
            Log.e("PokeGoBR-Scanner", "Falha no pipeline de OCR", error)
            updateStatus("⚠ Leitura indisponível", "Falha no OCR. Pare e inicie uma nova captura.")
            bitmap.recycle()
            busy.set(false)
        }
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
            text = "◉ Scanner"; textSize = 13f; setTextColor(Color.WHITE)
            setPadding(dp(10), dp(8), dp(10), dp(8))
        }
        bubbleTitle = handle
        root.addView(handle)
        val details = LinearLayout(this).apply {
            // Closed by default: never cover the appraisal bars or name.
            orientation = LinearLayout.VERTICAL; visibility = View.GONE
            setPadding(dp(10), 0, dp(10), dp(9))
        }
        textView = TextView(this).apply {
            text = "Abra Avaliar. Os dados detalhados aparecerão aqui."
            textSize = 12f; setTextColor(Color.rgb(230, 249, 255))
        }
        details.addView(textView)

        val scanModeRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, dp(8), 0, dp(4))
        }
        val autoMode = TextView(this).apply {
            text = "↻  Automático"
            textSize = 12f
            setTextColor(Color.rgb(160, 232, 228))
            setPadding(dp(5), dp(6), dp(12), dp(6))
            setOnClickListener {
                resumeAutomaticAfterMenu = true
                updateStatus("◉ Automático",
                    "Modo automático selecionado. Feche este painel para retomar a leitura.")
            }
        }
        val tapMode = TextView(this).apply {
            text = "◎  Por toque"
            textSize = 12f
            setTextColor(Color.WHITE)
            setPadding(dp(5), dp(6), dp(5), dp(6))
            setOnClickListener {
                resumeAutomaticAfterMenu = false
                scanGate.setAutomatic(false)
                updateStatus("◉ Por toque",
                    "Toque em Escanear agora para analisar um quadro da tela.")
            }
        }
        scanModeRow.addView(autoMode)
        scanModeRow.addView(tapMode)
        details.addView(scanModeRow)
        val scanNow = TextView(this).apply {
            text = "◉  Escanear agora"
            textSize = 13f
            setTextColor(Color.rgb(156, 232, 226))
            setPadding(0, dp(6), 0, dp(6))
            setOnClickListener {
                // Hide controls before requesting the next frame to reduce OCR
                // contamination from our own expanded overlay.
                details.visibility = View.GONE
                scanGate.setAutomatic(resumeAutomaticAfterMenu)
                scanGate.requestScan()
                updateStatus("◉ Escaneando",
                    "Aguardando quadro do Pokémon, sem o painel de controles aberto.")
            }
        }
        details.addView(scanNow)
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
            // Overlay visible in screenshots and permitted MediaProjection captures.
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply { gravity = Gravity.TOP or Gravity.START; x = dp(12); y = dp(82) }
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
                    MotionEvent.ACTION_UP -> if (!moved) {
                        if (details.visibility == View.VISIBLE) {
                            details.visibility = View.GONE
                            scanGate.setAutomatic(resumeAutomaticAfterMenu)
                        } else {
                            resumeAutomaticAfterMenu = scanGate.isAutomatic()
                            // Pause continuous scans while our controls are open:
                            // otherwise their own text can corrupt Pokémon OCR.
                            scanGate.setAutomatic(false)
                            details.visibility = View.VISIBLE
                        }
                    }
                }
                return true
            }
        })
        try {
            manager?.addView(root, params)
            overlay = root
        } catch (error: Exception) {
            throw IllegalStateException("Não foi possível abrir bolha flutuante", error)
        }
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
        captureWorker = null
        bubbleTitle = null
        textView = null
        runCatching { recognizer.close() }
        super.onDestroy()
    }
}
