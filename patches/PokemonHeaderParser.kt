package br.com.pokegobr

/**
 * Positions are fractions of the screen (0..1). Never infer a Pokémon's CP
 * from OCR elsewhere in the app, such as candies or our floating controls.
 */
internal data class PokemonHeader(val species: String?, val cp: Int?, val hp: Int?)

internal object PokemonHeaderParser {
    private val cpPattern = Regex("""(?i)\b(?:PC|CP)\s*[:#\-]?\s*(\d{2,5})\b""")
    private val cpOnly = Regex("""(?i)^\s*(?:PC|CP)\s*:?\s*$""")
    private val numeric = Regex("""^\s*(\d{2,5})\s*$""")
    private val hpFraction = Regex("""(?i)\b(\d{1,4})\s*/\s*(\d{1,4})\s*(?:PS|HP)\b""")
    private val hpDirect = Regex("""(?i)\b(?:PS|HP)\s*:?\s*(\d{1,4})\b""")

    fun parse(lines: List<SpatialOcrLine>, speciesNames: Collection<String>): PokemonHeader {
        val top = lines.filter {
            it.relativeY in .005f.. .155f && it.relativeX in .20f.. .82f
        }
        val cp = top.mapNotNull { line ->
            cpPattern.find(line.text)?.groupValues?.get(1)?.toIntOrNull()
                ?.takeIf { it in 10..99999 }
        }.firstOrNull() ?: run {
            // "PC" and "152" may be separate OCR elements at nearly the same Y.
            top.asSequence().filter { cpOnly.matches(it.text) }
                .mapNotNull { marker ->
                    top.asSequence().filter {
                        it !== marker && kotlin.math.abs(it.relativeY - marker.relativeY) <= .022f &&
                            it.relativeX > marker.relativeX &&
                            it.relativeX - marker.relativeX <= .28f
                    }.mapNotNull { number ->
                        numeric.matchEntire(number.text)?.groupValues?.get(1)
                            ?.toIntOrNull()?.takeIf { it in 10..99999 }
                    }.firstOrNull()
                }.firstOrNull()
        }
        // HP is immediately below the name plate. Do not pick numbers in a
        // floating overlay or in the Stardust/Candy rows lower on the screen.
        val hp = lines.asSequence()
            .filter { it.relativeY in .375f.. .51f && it.relativeX in .21f.. .91f }
            .mapNotNull { line ->
                val fraction = hpFraction.find(line.text)
                if (fraction != null) {
                    val current = fraction.groupValues[1].toIntOrNull()
                    val full = fraction.groupValues[2].toIntOrNull()
                    if (current != null && full != null && current in 1..9999 &&
                        full in 1..9999 && current <= full) full else null
                } else {
                    hpDirect.find(line.text)?.groupValues?.get(1)?.toIntOrNull()
                        ?.takeIf { it in 1..9999 }
                }
            }.firstOrNull()
        return PokemonHeader(
            SpatialSpeciesResolver.resolve(lines, speciesNames), cp, hp
        )
    }
}
