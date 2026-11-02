package br.com.pokegobr

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Raid OCR confidence tests. No network or screenshots in repository. */
class RaidScreenParserTest {
    @Test fun PortugueseRaidWithExplicitFields() {
        val raid = RaidScreenParser.parse(
            "CHEFE DE REIDE: Gengar\nTEMPO RESTANTE: 00:48\nTREINADORES: 3/5"
        )
        assertTrue(raid.recognized)
        assertEquals("GENGAR", raid.boss)
        assertEquals("00:48", raid.timeRemaining)
        assertEquals("3/5", raid.players)
    }

    @Test fun EnglishRaidWithExplicitFields() {
        val raid = RaidScreenParser.parse(
            "Raid Boss: Giratina\nTime Left 01:23\nTrainers 4/6"
        )
        assertTrue(raid.recognized)
        assertEquals("GIRATINA", raid.boss)
        assertEquals("01:23", raid.timeRemaining)
        assertEquals("4/6", raid.players)
    }

    @Test fun AGenericRaidWithoutFieldsNeverInventsBoss() {
        val raid = RaidScreenParser.parse("Batalha de Reide\nEntrar na Reide")
        assertTrue(raid.recognized)
        assertNull(raid.boss)
        assertNull(raid.timeRemaining)
        assertNull(raid.players)
    }

    @Test fun ARegularPokemonScreenMustNotBeCalledARaid() {
        val raid = RaidScreenParser.parse("Haunter\nPC 1499\nPS 101\nAvaliar")
        assertFalse(raid.recognized)
        assertNull(raid.boss)
    }

    @Test fun PokeRaidAppMenuMustNotBeMistakenForAGameRaid() {
        val raid = RaidScreenParser.parse("PokeRaid\nSalas\nSeu Pokémon\nPokédex")
        assertFalse(raid.recognized)
    }

    @Test fun UnlabelledTimeAndCpAreNotRaidFields() {
        val raid = RaidScreenParser.parse("Batalha de Reide\n21:45\nPC 3445")
        assertTrue(raid.recognized)
        assertNull(raid.timeRemaining)
        assertNull(raid.boss)
    }

    @Test fun InvalidClockAndPlayerCountsAreRejected() {
        val raid = RaidScreenParser.parse(
            "RAID LOBBY\nTIME LEFT 12:86\nPLAYERS 30/5"
        )
        assertTrue(raid.recognized)
        assertNull(raid.timeRemaining)
        assertNull(raid.players)
    }

    @Test fun NameOfBossMustBeExplicitlyLabelled() {
        val raid = RaidScreenParser.parse("Chefe de Reide\nGiratina\nTempo restante: 0:20")
        assertTrue(raid.recognized)
        assertNull(raid.boss)
        assertEquals("00:20", raid.timeRemaining)
    }

    @Test fun DisplayAlwaysDisclosesOcrUncertainty() {
        val raid = RaidScreenParser.parse("Raid Boss: Pikachu\nTime Left 0:08")
        assertTrue(raid.display().contains("conferir"))
        assertTrue(raid.display().contains("Verifique"))
    }
}
