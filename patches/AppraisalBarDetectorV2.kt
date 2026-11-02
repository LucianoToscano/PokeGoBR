package br.com.pokegobr

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/** Result is only a tentative visual estimate, never an automatically verified appraisal. */
internal data class AppraisalBarPreview(
    val barRowsFound: Int = 0,
    val proposedIvs: List<Int>? = null,
    val description: String = "Nenhuma barra de avaliação identificada."
)

/** Original pixel detector based on the whole 0..15 appraisal track, not on fill runs. */
internal object AppraisalBarDetector {
    private data class ScanRow(val y: Int, val left: Int, val right: Int) {
        val width: Int get() = right - left + 1
    }
    private data class BarTrack(var firstY: Int, var lastY: Int, var left: Int, var right: Int, var bestY: Int) {
        val width: Int get() = right - left + 1
        val centerY: Int get() = bestY
    }

    fun inspect(context: Context, uri: Uri): AppraisalBarPreview = runCatching {
        val bitmap = decodeSmall(context, uri) ?: return AppraisalBarPreview()
        try { inspectBitmap(bitmap) } finally { bitmap.recycle() }
    }.getOrElse { AppraisalBarPreview(description = "Não foi possível analisar as barras desta captura.") }

    private fun decodeSmall(context: Context, uri: Uri): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        if (bounds.outWidth < 150 || bounds.outHeight < 300) return null
        var sample = 1
        while (max(bounds.outWidth, bounds.outHeight) / sample > 1750) sample *= 2
        val options = BitmapFactory.Options().apply {
            inSampleSize = sample
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        return context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
    }

    private fun fill(c: Int): Boolean {
        val r = (c ushr 16) and 255
        val g = (c ushr 8) and 255
        val b = c and 255
        // Red/pink at high appraisal, peach/orange at lower values.
        return r > 190 && r - g > 47 && r - b > 40 && g in 76..219 && b < 220
    }

    private fun track(c: Int): Boolean {
        if (fill(c)) return true
        val r = (c ushr 16) and 255
        val g = (c ushr 8) and 255
        val b = c and 255
        return r in 151..239 && max(r, max(g, b)) - min(r, min(g, b)) < 11
    }

    fun inspectBitmap(bitmap: Bitmap): AppraisalBarPreview {
        val w = bitmap.width
        val h = bitmap.height
        if (w < 150 || h < 300) return AppraisalBarPreview()
        val x1 = (w * .05f).toInt()
        val x2 = (w * .70f).toInt().coerceAtMost(w - 1)
        val minTrack = (w * .24f).toInt()
        val maxTrack = (w * .60f).toInt()
        val maxGap = max(4, (w * .013f).toInt())
        val edgeTolerance = max(5, (w * .025f).toInt())
        val rows = ArrayList<ScanRow>()
        for (y in (h * .35f).toInt() until (h * .94f).toInt() step 2) {
            var first = -1
            var last = -1
            var gap = 0
            var best: ScanRow? = null
            for (x in x1..x2) {
                if (track(bitmap.getPixel(x, y))) {
                    if (first < 0) first = x
                    last = x
                    gap = 0
                } else if (first >= 0) {
                    if (++gap > maxGap) {
                        val len = last - first + 1
                        if (len in minTrack..maxTrack && (best == null || len > best.width))
                            best = ScanRow(y, first, last)
                        first = -1
                        last = -1
                        gap = 0
                    }
                }
            }
            if (first >= 0) {
                val len = last - first + 1
                if (len in minTrack..maxTrack && (best == null || len > best.width))
                    best = ScanRow(y, first, last)
            }
            if (best != null) rows.add(best)
        }
        if (rows.isEmpty()) return AppraisalBarPreview()

        val tracks = ArrayList<BarTrack>()
        for (row in rows) {
            val group = tracks.lastOrNull()
            if (group != null && row.y - group.lastY <= 4 &&
                abs(row.left - group.left) <= edgeTolerance && abs(row.right - group.right) <= edgeTolerance) {
                group.lastY = row.y
                if (row.width > group.width) {
                    group.left = row.left
                    group.right = row.right
                    group.bestY = row.y
                }
            } else {
                tracks.add(BarTrack(row.y, row.y, row.left, row.right, row.y))
            }
        }
        val filtered = tracks.filter { it.lastY - it.firstY >= max(3, (h * .003f).toInt()) &&
            it.lastY - it.firstY <= h * .020f }
        var selected: List<BarTrack>? = null
        var bestScore = -1
        for (a in filtered.indices) {
            val first = filtered[a]
            for (b in a + 1 until filtered.size) {
                val middle = filtered[b]
                val gap1 = middle.centerY - first.centerY
                if (gap1 < h * .025f || gap1 > h * .12f) continue
                for (c in b + 1 until filtered.size) {
                    val last = filtered[c]
                    val gap2 = last.centerY - middle.centerY
                    if (abs(gap1 - gap2) > max(6f, max(gap1, gap2) * .24f)) continue
                    if (max(abs(first.left - middle.left), abs(first.left - last.left)) > edgeTolerance) continue
                    if (max(abs(first.right - middle.right), abs(first.right - last.right)) > edgeTolerance) continue
                    val score = first.width + middle.width + last.width
                    if (score > bestScore) {
                        bestScore = score
                        selected = listOf(first, middle, last)
                    }
                }
            }
        }
        val found = selected ?: return AppraisalBarPreview(description =
            "Faixas encontradas, mas não há três barras alinhadas e completas. Revise manualmente.")

        val estimated = found.map { bar ->
            val y = bar.centerY.coerceIn(2, h - 3)
            var firstFill = -1
            var lastFill = -1
            var filledCount = 0
            for (x in bar.left..bar.right) {
                var nearFill = 0
                for (dy in -2..2) if (fill(bitmap.getPixel(x, y + dy))) nearFill++
                if (nearFill >= 2) {
                    if (firstFill < 0) firstFill = x
                    lastFill = x
                    filledCount++
                }
            }
            if (firstFill < 0) 0
            else if (firstFill - bar.left > max(5, (w * .012f).toInt())) null
            else if (filledCount.toFloat() / (lastFill - firstFill + 1).coerceAtLeast(1) < .78f) null
            else (15f * (lastFill - bar.left + 1) / bar.width).roundToInt().coerceIn(0, 15)
        }
        if (estimated.any { it == null }) return AppraisalBarPreview(
            barRowsFound = 3,
            description = "Barras localizadas, mas alguma foi parcialmente coberta. Confirme os IVs manualmente."
        )
        return AppraisalBarPreview(
            barRowsFound = 3,
            proposedIvs = estimated.filterNotNull(),
            description = "Leitura visual experimental de três barras. Revise os IVs antes de aplicar."
        )
    }
}
