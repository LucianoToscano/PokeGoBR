package br.com.pokegobr

import java.text.Normalizer
import java.util.Locale

/**
 * Prevents a single noisy OCR/bitmap frame from being presented as a stable
 * automatic IV reading. Stable means repeated pixel estimates, NOT verified IV.
 */
internal sealed class IvReadiness {
    data class Waiting(val count: Int, val needed: Int) : IvReadiness()
    data class Ready(val frames: Int) : IvReadiness()
}

internal class IvScanConsensus(
    private val needed: Int = 2,
    private val maxGapMs: Long = 8000L
) {
    init { require(needed >= 1 && maxGapMs > 0) }
    private data class Fingerprint(val species: String?, val cp: Int?, val iv: List<Int>)
    private var last: Fingerprint? = null
    private var lastAt = -1L
    private var count = 0

    @Synchronized
    fun observe(
        species: String?, cp: Int?, iv: List<Int>,
        elapsedMs: Long, automatic: Boolean
    ): IvReadiness {
        if (iv.size != 3 || iv.any { it !in 0..15 }) {
            reset()
            return IvReadiness.Waiting(0, if (automatic) needed else 1)
        }
        if (!automatic) {
            reset()
            return IvReadiness.Ready(1)
        }
        val now = Fingerprint(species?.lowercase(Locale.ROOT), cp, iv.toList())
        val prior = last
        val compatible = prior != null && elapsedMs >= lastAt &&
            elapsedMs - lastAt <= maxGapMs && prior.iv == now.iv &&
            (prior.cp == null || now.cp == null || prior.cp == now.cp) &&
            (prior.species == null || now.species == null || prior.species == now.species)
        count = if (compatible) count + 1 else 1
        last = now
        lastAt = elapsedMs
        if (count >= needed) {
            val seen = count
            reset()
            return IvReadiness.Ready(seen)
        }
        return IvReadiness.Waiting(count, needed)
    }

    @Synchronized
    fun reset() {
        last = null
        lastAt = -1L
        count = 0
    }
}

internal data class SpatialOcrLine(
    val text: String,
    val relativeY: Float,
    val relativeX: Float
)

/**
 * A Pokémon's name appears around the name plate. Avoid species names in
 * lower text such as candy/evolution labels ("Energia Greninja") and OCR of
 * the whole screen. No name is preferable to a wrong one.
 */
internal object SpatialSpeciesResolver {
    private val words = Regex("""[\p{L}]{3,35}(?:[-'][\p{L}]{2,35})?""")
    private fun fold(text: String): String =
        Normalizer.normalize(text, Normalizer.Form.NFD)
            .replace(Regex("""\p{M}+"""), "")
            .lowercase(Locale.ROOT)

    fun resolve(lines: List<SpatialOcrLine>, candidates: Collection<String>): String? {
        if (lines.isEmpty() || candidates.isEmpty()) return null
        val lookup = candidates.associateBy(::fold)
        return lines.asSequence()
            .filter { it.relativeY in 0.26f..0.55f && it.relativeX in 0.12f..0.91f }
            .mapNotNull { line ->
                val name = words.findAll(line.text).mapNotNull {
                    lookup[fold(it.value)]
                }.firstOrNull()
                name?.let { it to kotlin.math.abs(line.relativeY - .40f) }
            }
            .minByOrNull { it.second }?.first
    }
}
