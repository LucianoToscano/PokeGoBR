package br.com.pokegobr

import org.junit.Assert.*
import org.junit.Test

class LiveIvResultTest {
    @Test fun haunterPvPGoodIvIsNotCalledPerfectOverall() {
        val result = LiveIvResult(
            species = "Haunter", cp = 1499, hp = 101, iv = listOf(1, 15, 14),
            pvpFromImports = listOf("GL #1 (Poke Genie)")
        )
        assertEquals(30, result.total)
        assertEquals(67, result.ivPercent)
        assertEquals(2, result.starCount)
        assertEquals("Haunter:1499:1/15/14", result.key())
        assertEquals(1, result.pvpFromImports.size)
    }
    @Test fun perfectIvHasHundredPercent() {
        val result = LiveIvResult("Dragonite", 4262, 183, listOf(15, 15, 15))
        assertEquals(100, result.ivPercent)
        assertEquals(4, result.starCount)
        assertTrue(result.pvpFromImports.isEmpty())
    }
    @Test fun lowIvAndMissingSpeciesRemainHonest() {
        val result = LiveIvResult(null, null, null, listOf(3, 15, 12))
        assertEquals(67, result.ivPercent)
        assertNull(result.species)
        assertNull(result.cp)
    }
    @Test fun notEnoughBarsMustNotGenerateValidResult() {
        try {
            LiveIvResult("Froakie", 281, 58, listOf(3, 15))
            fail("IV must have exactly 3 attributes")
        } catch (_: IllegalArgumentException) { }
        try {
            LiveIvResult("Froakie", 281, 58, listOf(3, 16, 12))
            fail("IV must be within 0 to 15")
        } catch (_: IllegalArgumentException) { }
    }
}
