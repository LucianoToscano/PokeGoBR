package br.com.pokegobr

import org.junit.Assert.*
import org.junit.Test

class ScannerReadoutParserTest {
    private val species = listOf("Froakie", "Greninja", "Mewtwo", "Gengar", "Charizard")

    @Test fun recognizesFroakieWithNicknameIvs() {
        val result = ScannerReadoutParser.parse(
            "PC281\nFroakie③⑮⑫\n58 / 58 PS\nPESO\n3,12kg\nALTURA", species
        )
        assertEquals("Froakie", result.species)
        assertEquals(281, result.cp)
        assertEquals(58, result.hp)
    }

    @Test fun neverUsesPesoAsSpecies() {
        val result = ScannerReadoutParser.parse(
            "PC 281\n58 / 58 PS\n3,12kg\nPESO\nÁGUA\nALTURA", species
        )
        assertNull(result.species)
        assertEquals(281, result.cp)
        assertEquals(58, result.hp)
    }

    @Test fun supportsEnglishCpAndHp() {
        val r = ScannerReadoutParser.parse("CP 1499\nHaunter\nHP 84", species + "Haunter")
        assertEquals("Haunter", r.species)
        assertEquals(1499, r.cp)
        assertEquals(84, r.hp)
    }

    @Test fun rejectsInvalidHpFraction() {
        val r = ScannerReadoutParser.parse("PC 1222\n180 / 60 PS", species)
        assertNull(r.hp)
    }

    @Test fun rejectsInvalidCp() {
        val r = ScannerReadoutParser.parse("PC 999999\nPS 50", species)
        assertNull(r.cp)
        assertEquals(50, r.hp)
    }

    @Test fun noIvsAreInventedFromInconsistentValues() {
        assertNull(ScannerReadoutParser.tentativeIvLabel(listOf(15, 15)))
        assertNull(ScannerReadoutParser.tentativeIvLabel(listOf(16, 15, 15)))
        assertEquals("≈3/15/12 · ≈66% · 2★",
            ScannerReadoutParser.tentativeIvLabel(listOf(3, 15, 12)))
        assertEquals("≈15/15/15 · ≈100% · 4★",
            ScannerReadoutParser.tentativeIvLabel(listOf(15, 15, 15)))
    }

    @Test fun normalizesCaseAndAccents() {
        val r = ScannerReadoutParser.parse("PC 410\nPOKÉMON\nFROAKIE", species)
        assertEquals("Froakie", r.species)
    }

    @Test fun noNameWhenIndexLacksSpecies() {
        val r = ScannerReadoutParser.parse("PC 281\nFroakie\n58 PS", listOf("Mewtwo"))
        assertNull(r.species)
    }
}
