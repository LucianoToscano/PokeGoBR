package br.com.pokegobr

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Conservative, local-only visual hint detector for the three orange appraisal bars.
 * It never marks an IV as confirmed. Users must explicitly accept and review hints.
 * No network access, game hooks, accessibility automation or third-party code.
 */
internal data class AppraisalBarPreview(
    val barRowsFound: Int = 0,
    val proposedIvs: List<Int>? = null,
    val description: String = "Nenhuma barra de avaliação identificada."
)

internal object AppraisalBarDetector {
    private data class Stripe(val y: Int, val left: Int, val right: Int) {
        val length: Int get() = right - left + 1
    }

    fun inspect(context: Context, uri: Uri): AppraisalBarPreview = runCatching {
        val bitmap = decodeSmall(context, uri) ?: return AppraisalBarPreview()
        try { inspectBitmap(bitmap) } finally { bitmap.recycle() }
    }.getOrElse { AppraisalBarPreview(description = "Não foi possível inspecionar as barras desta imagem.") }

    private fun decodeSmall(context: Context, uri: Uri): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
        var sample = 1
        while (max(bounds.outWidth, bounds.outHeight) / sample > 1550) sample *= 2
        val opts = BitmapFactory.Options().apply { inSampleSize = sample; inPreferredConfig = Bitmap.Config.ARGB_8888 }
        return context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts) }
    }

    private fun orange(c: Int): Boolean {
        val r = (c shr 16) and 255
        val g = (c shr 8) and 255
        val b = c and 255
        return r >= 145 && r > g * 1.20f && r > b * 1.18f && g in 37..210 && b < 195
    }

    private fun paleTrack(c: Int): Boolean {
        val r = (c shr 16) and 255
        val g = (c shr 8) and 255
        val b = c and 255
        return min(r, min(g, b)) > 105 && max(r, max(g, b)) - min(r, min(g, b)) < 53
    }

    private fun inspectBitmap(b: Bitmap): AppraisalBarPreview {
        val w = b.width; val h = b.height
        if (w < 150 || h < 300) return AppraisalBarPreview(description = "Imagem pequena demais para identificar as barras.")
        val startX = (w * .10f).toInt(); val stopX = (w * .87f).toInt()
        val minLength = max(16, (w * .065f).toInt())
        val stripes = ArrayList<Stripe>()
        for (y in (h * .22f).toInt() until (h * .94f).toInt() step 2) {
            var runStart = -1
            for (x in startX..stopX) {
                val hit = orange(b.getPixel(x, y))
                if (hit && runStart < 0) runStart = x
                if ((!hit || x == stopX) && runStart >= 0) {
                    val end = if (hit) x else x - 1
                    if (end - runStart + 1 >= minLength) stripes.add(Stripe(y, runStart, end))
                    runStart = -1
                }
            }
        }
        if (stripes.isEmpty()) return AppraisalBarPreview()
        val groups = ArrayList<Stripe>()
        for (s in stripes.sortedBy { it.y }) {
            val idx = groups.indexOfLast { abs(it.y - s.y) <= max(5, h / 150) && abs(it.left - s.left) < w * .035f }
            if (idx >= 0) {
                if (s.length > groups[idx].length) groups[idx] = s
            } else groups.add(s)
        }
        val candidates = groups.sortedBy { it.y }
        var best: List<Stripe>? = null
        var bestScore = -1
        for (a in 0 until candidates.size) for (c in a + 2 until candidates.size) {
            val first = candidates[a]; val last = candidates[c]
            val space = (last.y - first.y) / 2f
            if (space < h * .025f || space > h * .115f) continue
            for (j in a + 1 until c) {
                val mid = candidates[j]
                if (abs((mid.y - first.y) - space) > max(5f, space * .27f)) continue
                if (max(abs(first.left - mid.left), abs(first.left - last.left)) > w * .045f) continue
                val score = first.length + mid.length + last.length
                if (score > bestScore) { bestScore = score; best = listOf(first, mid, last) }
            }
        }
        val rows = best ?: return AppraisalBarPreview(
            barRowsFound = min(2, groups.size),
            description = "Faixas coloridas encontradas, mas sem três barras alinhadas. Confirme os IVs manualmente."
        )
        val trackEnds = rows.map { s ->
            var x = s.right + 1; var last = s.right; var gaps = 0
            val limit = min(stopX, s.left + (w * .65f).toInt())
            while (x <= limit) {
                if (paleTrack(b.getPixel(x, s.y)) || orange(b.getPixel(x, s.y))) {
                    last = x; gaps = 0
                } else if (++gaps > max(3, w / 180)) break
                x++
            }
            if (last - s.right >= w * .045f) last else null
        }
        val validEnds = trackEnds.filterNotNull()
        val trackRight = if (validEnds.size >= 2 && validEnds.maxOrNull()!! - validEnds.minOrNull()!! < w * .035f)
            validEnds.sorted()[validEnds.size / 2] else null
        val estimates = trackRight?.let { right ->
            val lengths = rows.map { right - it.left + 1 }
            if (lengths.minOrNull()!! >= w * .17f && lengths.maxOrNull()!! <= w * .68f)
                rows.mapIndexed { i, s -> (15f * s.length / lengths[i]).roundToInt().coerceIn(0, 15) }
            else null
        }
        return if (estimates != null) AppraisalBarPreview(
            barRowsFound = 3,
            proposedIvs = estimates,
            description = "Três barras detectadas. Valores aproximados pela imagem; revise antes de aplicar."
        ) else AppraisalBarPreview(
            barRowsFound = 3,
            description = "Três barras detectadas, mas o comprimento total não ficou claro. Para evitar IV incorreto, informe os valores manualmente."
        )
    }
}
