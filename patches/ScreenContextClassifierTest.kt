package br.com.pokegobr

import org.junit.Assert.assertEquals
import org.junit.Test

/** Pure Kotlin unit tests; no screen capture, private screenshots or network. */
class ScreenContextClassifierTest {
    @Test fun appraisalWithThreeBars() {
        val found = ScreenContextClassifier.classify("Avaliação Ataque Defesa PS", 3, 1499, 90)
        assertEquals(ScanScene.APPRAISAL, found.scene)
        assertEquals(SceneConfidence.HIGH, found.confidence)
    }

    @Test fun highIVBarsWithoutRecognizedAppraisalTextAreTentative() {
        val found = ScreenContextClassifier.classify("CP 4262 HP 180", 3, 4262, 180)
        assertEquals(ScanScene.APPRAISAL, found.scene)
        assertEquals(SceneConfidence.MEDIUM, found.confidence)
    }

    @Test fun evaluateButtonOnDetailsDoesNotPretendAnAppraisal() {
        val found = ScreenContextClassifier.classify("Avaliar\nFortalecer\nPC 1499\nPS 90", 0, 1499, 90)
        assertEquals(ScanScene.DETAILS, found.scene)
    }

    @Test fun detailsRecognizedWithCpAndHp() {
        val found = ScreenContextClassifier.classify("CP 2011 HP 113", 0, 2011, 113)
        assertEquals(ScanScene.DETAILS, found.scene)
        assertEquals(SceneConfidence.HIGH, found.confidence)
    }

    @Test fun partialDetailsNeedAnotherClue() {
        val found = ScreenContextClassifier.classify("PS 80", 0, null, 80)
        assertEquals(ScanScene.OTHER, found.scene)
        val partial = ScreenContextClassifier.classify("Fortalecer\nPS 80", 0, null, 80)
        assertEquals(ScanScene.DETAILS, partial.scene)
        assertEquals(SceneConfidence.MEDIUM, partial.confidence)
    }

    @Test fun raidRecognizedByContextNotJustMenuName() {
        val found = ScreenContextClassifier.classify("Chefe de Reide\nGrupo de batalha", 0, null, null)
        assertEquals(ScanScene.RAID, found.scene)
        val menu = ScreenContextClassifier.classify("PokeGoBR\nRaides\nCalculadora", 0, null, null)
        assertEquals(ScanScene.OTHER, menu.scene)
    }

    @Test fun movesRecognizedInPortugueseAndEnglish() {
        val pt = ScreenContextClassifier.classify("Ataque Rápido\nAtaque Carregado", 0, null, null)
        assertEquals(ScanScene.MOVES, pt.scene)
        assertEquals(SceneConfidence.HIGH, pt.confidence)
        val en = ScreenContextClassifier.classify("Fast Attack", 0, null, null)
        assertEquals(ScanScene.MOVES, en.scene)
        assertEquals(SceneConfidence.MEDIUM, en.confidence)
    }

    @Test fun noDataNeverInventsAppraisal() {
        val none = ScreenContextClassifier.classify("Pokémon GO\nAVALIAR", 0, null, null)
        assertEquals(ScanScene.OTHER, none.scene)
        assertEquals(SceneConfidence.LOW, none.confidence)
    }

    @Test fun screenshotTextIsNormalizedForAccentsAndCase() {
        val found = ScreenContextClassifier.classify("BATALHA DE REIDE\nchefe", 0, null, null)
        assertEquals(ScanScene.RAID, found.scene)
    }
}
