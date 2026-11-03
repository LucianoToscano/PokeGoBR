package br.com.pokegobr

import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TextView
import kotlin.math.abs
import kotlin.math.roundToInt

/** A minimal user-driven IV result card; never claims an estimated reading is verified. */
internal data class LiveIvResult(
    val species: String?,
    val cp: Int?,
    val hp: Int?,
    val iv: List<Int>,
    val pvpFromImports: List<String> = emptyList(),
    val userConfirmed: Boolean = false,
    val manuallyEdited: Boolean = false
) {
    init { require(iv.size == 3 && iv.all { it in 0..15 }) }
    val total: Int get() = iv.sum()
    val ivPercent: Int get() = (total * 100.0 / 45.0).roundToInt()
    val starCount: Int get() = when {
        total == 45 -> 4
        total >= 37 -> 3
        total >= 30 -> 2
        total >= 23 -> 1
        else -> 0
    }
    fun key(): String = (species ?: "?") + ":" + (cp ?: 0) + ":" + iv.joinToString("/")
}

internal class LiveIvResultCard(
    private val context: Context,
    private val windowManager: WindowManager,
    private val onRescan: () -> Unit,
    private val onSave: (LiveIvResult) -> Unit
) {
    private val density = context.resources.displayMetrics.density
    private fun dp(value: Float): Int = (value * density).roundToInt()
    private val ink = Color.rgb(36, 51, 55)
    private val faint = Color.rgb(99, 118, 119)
    private val accent = Color.rgb(35, 139, 121)
    private val accentSoft = Color.rgb(229, 247, 241)
    private var root: LinearLayout? = null
    private var layoutParams: WindowManager.LayoutParams? = null
    private var lastResult: LiveIvResult? = null
    private var titleView: TextView? = null
    private var percentView: TextView? = null
    private var subtitleView: TextView? = null
    private var ivViews: List<TextView> = emptyList()
    private var pvpView: TextView? = null
    private var hintView: TextView? = null
    private var confirmView: TextView? = null

    private fun shape(color: Int, radius: Float, border: Int? = null): GradientDrawable =
        GradientDrawable().apply {
            setColor(color)
            cornerRadius = dp(radius).toFloat()
            if (border != null) setStroke(dp(1f), border)
        }

    private fun label(text: String, size: Float, color: Int = ink, bold: Boolean = false): TextView =
        TextView(context).apply {
            this.text = text
            textSize = size
            setTextColor(color)
            if (bold) setTypeface(typeface, Typeface.BOLD)
            gravity = Gravity.CENTER_VERTICAL
            includeFontPadding = false
        }

    private fun space(horizontal: Int = 0, vertical: Int = 0): View =
        View(context).apply {
            layoutParams = LinearLayout.LayoutParams(dp(horizontal.toFloat()), dp(vertical.toFloat()))
        }

    private fun build(): LinearLayout {
        val card = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            background = shape(Color.argb(246, 249, 252, 250), 17f, Color.argb(75, 42, 111, 101))
            elevation = dp(8f).toFloat()
            setPadding(dp(14f), dp(12f), dp(14f), dp(12f))
        }
        val header = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val titleColumn = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }
        val name = label("Pokémon", 18f, ink, bold = true).apply {
            maxLines = 1
            ellipsize = android.text.TextUtils.TruncateAt.END
        }
        titleView = name
        titleColumn.addView(name)
        val secondary = label("PC — · PS —", 11f, faint)
        subtitleView = secondary
        titleColumn.addView(secondary)
        header.addView(titleColumn, LinearLayout.LayoutParams(0,
            LinearLayout.LayoutParams.WRAP_CONTENT, 1f))

        val percent = label("—", 14f, accent, bold = true).apply {
            gravity = Gravity.CENTER
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(accentSoft)
                setStroke(dp(2.5f), accent)
            }
            setPadding(dp(4f), dp(4f), dp(4f), dp(4f))
        }
        percentView = percent
        header.addView(percent, LinearLayout.LayoutParams(dp(53f), dp(53f)))
        card.addView(header)
        card.addView(space(vertical = 7))
        val divider = View(context).apply { setBackgroundColor(Color.argb(33, 45, 78, 70)) }
        card.addView(divider, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, dp(1f)))

        val ivRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(0, dp(10f), 0, dp(5f))
        }
        val numberViews = ArrayList<TextView>()
        listOf("ATAQUE", "DEFESA", "PS").forEachIndexed { index, nameText ->
            val column = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
            }
            column.addView(label(nameText, 9f, faint).apply { gravity = Gravity.CENTER })
            column.addView(space(vertical = 3))
            val number = label("—/15", 17f, ink, bold = true).apply { gravity = Gravity.CENTER }
            numberViews.add(number)
            column.addView(number)
            val editorRow = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER
            }
            val minus = label("−", 19f, accent, bold = true).apply {
                gravity = Gravity.CENTER
                contentDescription = "Diminuir IV de $nameText"
                setOnClickListener { adjustIv(index, -1) }
            }
            val plus = label("+", 19f, accent, bold = true).apply {
                gravity = Gravity.CENTER
                contentDescription = "Aumentar IV de $nameText"
                setOnClickListener { adjustIv(index, 1) }
            }
            editorRow.addView(minus, LinearLayout.LayoutParams(dp(36f), dp(34f)))
            editorRow.addView(plus, LinearLayout.LayoutParams(dp(36f), dp(34f)))
            column.addView(editorRow)
            ivRow.addView(column, LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        }
        ivViews = numberViews
        card.addView(ivRow)
        val verified = label("☐ Conferi os IVs na avaliação", 10.5f, faint).apply {
            gravity = Gravity.CENTER
            setPadding(dp(5f), dp(5f), dp(5f), dp(7f))
            setOnClickListener {
                lastResult?.let { show(it.copy(userConfirmed = !it.userConfirmed)) }
            }
        }
        confirmView = verified
        card.addView(verified)
        val pvp = label("PvP: sem rank calculado", 10.5f, faint).apply {
            gravity = Gravity.CENTER
            maxLines = 2
            setPadding(dp(5f), dp(5f), dp(5f), dp(5f))
            background = shape(Color.argb(155, 230, 240, 236), 7f)
        }
        pvpView = pvp
        card.addView(pvp, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT))
        card.addView(space(vertical = 8))

        val actions = LinearLayout(context).apply { orientation = LinearLayout.HORIZONTAL }
        val close = label("Fechar", 11.5f, faint, true).apply {
            gravity = Gravity.CENTER
            setOnClickListener { hide() }
        }
        actions.addView(close, LinearLayout.LayoutParams(0, dp(34f), 1f))
        val scan = label("Novo scan", 11.5f, accent, true).apply {
            gravity = Gravity.CENTER
            background = shape(Color.argb(230, 224, 245, 239), 8f)
            setOnClickListener {
                hide()
                onRescan()
            }
        }
        actions.addView(scan, LinearLayout.LayoutParams(0, dp(34f), 1.2f))
        actions.addView(space(horizontal = 7))
        val save = label("Salvar", 11.5f, Color.WHITE, true).apply {
            gravity = Gravity.CENTER
            background = shape(accent, 8f)
            setOnClickListener {
                val snapshot = lastResult ?: return@setOnClickListener
                if (snapshot.species.isNullOrBlank() || snapshot.cp == null) {
                    hintView?.text = "Espécie/PC desconhecidos: não salvo"
                    return@setOnClickListener
                }
                onSave(snapshot)
                hintView?.text = if (snapshot.userConfirmed)
                    "Conferido pelo usuário · salvo em Meus Pokémon"
                else "Estimativa salva em Meus Pokémon"
            }
        }
        actions.addView(save, LinearLayout.LayoutParams(0, dp(34f), 1f))
        card.addView(actions)
        card.addView(space(vertical = 5))
        val hint = label("IV visual aproximado · confira antes de usar", 9.5f, faint)
            .apply { gravity = Gravity.CENTER }
        hintView = hint
        card.addView(hint)

        // Drag only by the header, keeping actions accessible.
        header.setOnTouchListener(object : View.OnTouchListener {
            private var originX = 0
            private var originY = 0
            private var rawX = 0f
            private var rawY = 0f
            override fun onTouch(v: View, event: MotionEvent): Boolean {
                val params = layoutParams ?: return false
                when (event.actionMasked) {
                    MotionEvent.ACTION_DOWN -> {
                        originX = params.x; originY = params.y
                        rawX = event.rawX; rawY = event.rawY
                        return true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        val dx = (event.rawX - rawX).roundToInt()
                        val dy = (event.rawY - rawY).roundToInt()
                        if (abs(dx) > dp(2f) || abs(dy) > dp(2f)) {
                            params.x = (originX + dx).coerceAtLeast(0)
                            params.y = (originY + dy).coerceAtLeast(0)
                            runCatching { windowManager.updateViewLayout(card, params) }
                        }
                        return true
                    }
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> return true
                }
                return false
            }
        })
        return card
    }

    private fun adjustIv(index: Int, delta: Int) {
        val result = lastResult ?: return
        if (index !in 0..2) return
        val corrected = result.iv.toMutableList()
        val changed = (corrected[index] + delta).coerceIn(0, 15)
        if (corrected[index] == changed) return
        corrected[index] = changed
        // Imported ranks refer to the original IVs, NOT the manually edited ones.
        show(result.copy(
            iv = corrected, userConfirmed = false, manuallyEdited = true,
            pvpFromImports = emptyList()
        ))
    }

    fun show(result: LiveIvResult) {
        lastResult = result
        val view = root ?: build().also { card ->
            root = card
            val metrics = if (Build.VERSION.SDK_INT >= 30)
                windowManager.maximumWindowMetrics.bounds
            else {
                val display = context.resources.displayMetrics
                android.graphics.Rect(0, 0, display.widthPixels, display.heightPixels)
            }
            val screenWidth = metrics.width().coerceAtLeast(dp(230f))
            val screenHeight = metrics.height().coerceAtLeast(dp(420f))
            val width = minOf(dp(318f), screenWidth - dp(24f))
            val params = WindowManager.LayoutParams(
                width, WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.TOP or Gravity.START
                x = (screenWidth - width) / 2
                y = (screenHeight - dp(268f)).coerceAtLeast(dp(110f))
            }
            layoutParams = params
            try {
                windowManager.addView(card, params)
            } catch (error: Exception) {
                root = null
                layoutParams = null
                throw error
            }
        }
        titleView?.text = result.species ?: "Pokémon não identificado"
        subtitleView?.text = "PC " + (result.cp ?: "—") + "  ·  PS " + (result.hp ?: "—")
        percentView?.text = result.ivPercent.toString() + "%"
        result.iv.forEachIndexed { index, value ->
            ivViews.getOrNull(index)?.text = "$value/15"
        }
        confirmView?.text = if (result.userConfirmed)
            "☑ IVs conferidos pelo usuário" else "☐ Conferi os IVs na avaliação"
        confirmView?.setTextColor(if (result.userConfirmed) accent else faint)
        pvpView?.text = if (result.pvpFromImports.isEmpty())
            "PvP: sem ranking calculado para esta espécie/IV"
        else result.pvpFromImports.take(2).joinToString("  ·  ")
        hintView?.text = "★".repeat(result.starCount) +
            if (result.userConfirmed) " · IVs conferidos por você"
            else if (result.manuallyEdited) " · Valores corrigidos; confira antes de salvar"
            else " · Estimativa visual; confira antes de salvar"
        view.visibility = View.VISIBLE
    }

    fun hide() { root?.visibility = View.GONE }

    fun dispose() {
        root?.let { runCatching { windowManager.removeView(it) } }
        root = null
        layoutParams = null
    }
}
