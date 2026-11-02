package br.com.pokegobr

import java.text.Normalizer
import java.util.Locale

/**
 * Conservative visual scan readout. A label from the details screen (e.g.
 * "PESO") is NOT a Pokémon name. Resolve only names in an installed species
 * index; missing data remains unknown instead of displaying invented names.
 */
internal data class ScannerReadout(val species: String?, val cp: Int?, val hp: Int?)

internal object ScannerReadoutParser {
    private val cpPattern = Regex("""(?i)(?:^|\b)(?:PC|CP)\s*[:#-]?\s*(\d{1,5})\b""")
    private val hpPattern = Regex("""(?i)(?:^|\b)(?:PS|HP)\s*[:#-]?\s*(\d{1,4})\b""")
    private val hpFraction = Regex("""(?i)\b(\d{1,4})\s*/\s*(\d{1,4})\s*(?:PS|HP)\b""")
    private val letterToken = Regex("""[\p{L}]{3,35}(?:[-'][\p{L}]{2,35})?""")

    private fun normalize(value: String): String =
        Normalizer.normalize(value, Normalizer.Form.NFD)
            .replace(Regex("""\p{M}+"""), "")
            .lowercase(Locale.ROOT)
            .trim()

    fun parse(text: String, speciesIndex: Collection<String>): ScannerReadout {
        val cp = cpPattern.find(text)?.groupValues?.getOrNull(1)?.toIntOrNull()
            ?.takeIf { it in 10..99999 }
        val hpFromFraction = hpFraction.find(text)?.let { match ->
            val current = match.groupValues[1].toIntOrNull()
            val total = match.groupValues[2].toIntOrNull()
            if (current != null && total != null && current in 1..9999 &&
                total in 1..9999 && current <= total) total else null
        }
        val hp = hpFromFraction ?: hpPattern.find(text)?.groupValues?.getOrNull(1)
            ?.toIntOrNull()?.takeIf { it in 1..9999 }

        val lookup = speciesIndex.filter { it.isNotBlank() }.associateBy(::normalize)
        // OCR may append circled nickname IVs: Froakie③⑮⑫. Read letters only.
        val name = letterToken.findAll(text).mapNotNull { token ->
            lookup[normalize(token.value)]
        }.firstOrNull()
        return ScannerReadout(name, cp, hp)
    }

    fun tentativeIvLabel(iv: List<Int>): String? {
        if (iv.size != 3 || iv.any { it !in 0..15 }) return null
        val sum = iv.sum()
        val stars = when {
            sum == 45 -> 4
            sum >= 37 -> 3
            sum >= 30 -> 2
            sum >= 23 -> 1
            else -> 0
        }
        val percent = (100.0 * sum / 45.0).toInt()
        return "≈" + iv.joinToString("/") + " · ≈" + percent + "% · " + stars + "★"
    }
}
