package br.com.pokegobr

import org.junit.Assert.*
import org.junit.Test

class PokemonHeaderParserTest {
    private val dex = listOf("Treecko", "Grovyle", "Sceptile", "Froakie", "Greninja")

    @Test fun treeckoPc152Hp43NotOverlayValues() {
        val lines = listOf(
            SpatialOcrLine("PC 152", .036f, .51f),
            SpatialOcrLine("◉ IV ≈82%", .251f, .28f),
            SpatialOcrLine("PC 942 · PS 143", .303f, .32f),
            SpatialOcrLine("Treecko⓪⑫⑮", .395f, .50f),
            SpatialOcrLine("43 / 43 PS", .439f, .50f),
            SpatialOcrLine("DOCE DE TREECKO", .80f, .52f)
        )
        val data = PokemonHeaderParser.parse(lines, dex)
        assertEquals("Treecko", data.species)
        assertEquals(152, data.cp)
        assertEquals(43, data.hp)
    }

    @Test fun acceptsTopCpAsTwoOcrFragments() {
        val data = PokemonHeaderParser.parse(listOf(
            SpatialOcrLine("PC", .035f, .39f),
            SpatialOcrLine("152", .037f, .53f),
            SpatialOcrLine("Froakie", .393f, .51f),
            SpatialOcrLine("58/58 PS", .448f, .50f)
        ), dex)
        assertEquals(152, data.cp)
        assertEquals(58, data.hp)
        assertEquals("Froakie", data.species)
    }

    @Test fun doNotGuessCpFromStardustOrOverlay() {
        val data = PokemonHeaderParser.parse(listOf(
            SpatialOcrLine("PC 942 PS 143", .285f, .25f),
            SpatialOcrLine("PC 281", .55f, .50f),
            SpatialOcrLine("Poeira estelar 235.015", .65f, .48f),
            SpatialOcrLine("Treecko", .39f, .50f)
        ), dex)
        assertNull(data.cp)
        assertNull(data.hp)
        assertEquals("Treecko", data.species)
    }

    @Test fun doNotGuessSpeciesFromCandyOrEvolution() {
        val data = PokemonHeaderParser.parse(listOf(
            SpatialOcrLine("Doce Greninja", .82f, .40f),
            SpatialOcrLine("PESO", .40f, .25f),
            SpatialOcrLine("152", .034f, .49f)
        ), dex)
        assertNull(data.species)
        assertNull(data.cp)
    }

    @Test fun rejectInvalidHpFractionAndNonMatchingNickname() {
        val data = PokemonHeaderParser.parse(listOf(
            SpatialOcrLine("PC 152", .039f, .51f),
            SpatialOcrLine("143/43 PS", .442f, .51f),
            SpatialOcrLine("DESCONHECIDO", .391f, .51f)
        ), dex)
        assertEquals(152, data.cp)
        assertNull(data.hp)
        assertNull(data.species)
    }
}
