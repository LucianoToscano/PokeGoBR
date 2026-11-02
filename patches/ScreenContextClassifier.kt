package br.com.pokegobr

import java.text.Normalizer
import java.util.Locale

/**
 * Independent screen-context classifier for consented Pokémon GO screenshots.
 *
 * This stage decides WHAT is visible. It does not calculate IV or identify a
 * species. Input comes only from local ML Kit OCR and the original bar detector.
 * An uncertain result must not become a confirmed observation.
 */
internal enum class ScanScene {
    APPRAISAL, DETAILS, MOVES, RAID, OTHER
}

internal enum class SceneConfidence {
    HIGH, MEDIUM, LOW
}

internal data class ScreenContext(
    val scene: ScanScene,
    val confidence: SceneConfidence,
    val reason: String
) {
    val title: String get() = when (scene) {
        ScanScene.APPRAISAL -> "Avaliação"
        ScanScene.DETAILS -> "Detalhes do Pokémon"
        ScanScene.MOVES -> "Golpes"
        ScanScene.RAID -> "Raide"
        ScanScene.OTHER -> "Sem Pokémon identificado"
    }
}

internal object ScreenContextClassifier {
    private val combiningMarks = Regex("\\p{M}+")
    private val spaces = Regex("\\s+")
    private val raidContext = Regex(
        "\\b(?:BATALHA DE REIDE|CHEFE DE REIDE|ENTRAR NA REIDE|" +
            "RAID BATTLE|RAID BOSS|RAID LOBBY|BOSS DE REIDE|CHEFE DA REIDE)\\b"
    )
    private val fastAttack = Regex("\\b(?:ATAQUE RAPIDO|FAST ATTACK|FAST MOVE|GOLPE RAPIDO)\\b")
    private val chargedAttack = Regex(
        "\\b(?:ATAQUE CARREGADO|CHARGED ATTACK|CHARGE MOVE|GOLPE CARREGADO)\\b"
    )
    private val appraisalText = Regex(
        "\\b(?:AVALIACAO|APPRAISAL|ESTATISTICAS DE COMBATE)\\b"
    )
    private val pokemonDetailsText = Regex(
        "\\b(?:FORTALECER|POWER UP|EVOLUIR|EVOLVE|POKEMON CAPTURADO|PESO|ALTURA)\\b"
    )

    fun classify(rawText: String, barRowsFound: Int, cp: Int?, hp: Int?): ScreenContext {
        val normalized = spaces.replace(
            Normalizer.normalize(rawText, Normalizer.Form.NFD)
                .replace(combiningMarks, "")
                .uppercase(Locale.ROOT),
            " "
        )
        val hasCp = cp != null && cp > 0
        val hasHp = hp != null && hp > 0

        // The button "AVALIAR" exists on the ordinary details screen; it is
        // intentionally NOT sufficient to label a screenshot as an appraisal.
        if (barRowsFound >= 3) {
            return ScreenContext(
                ScanScene.APPRAISAL,
                if (appraisalText.containsMatchIn(normalized)) SceneConfidence.HIGH
                else SceneConfidence.MEDIUM,
                "Três barras de avaliação detectadas visualmente"
            )
        }

        if (raidContext.containsMatchIn(normalized)) {
            return ScreenContext(
                ScanScene.RAID, SceneConfidence.MEDIUM,
                "Texto característico de uma tela de raide"
            )
        }

        val fast = fastAttack.containsMatchIn(normalized)
        val charged = chargedAttack.containsMatchIn(normalized)
        if (fast || charged) {
            return ScreenContext(
                ScanScene.MOVES,
                if (fast && charged) SceneConfidence.HIGH else SceneConfidence.MEDIUM,
                "Rótulos de golpes encontrados no OCR"
            )
        }

        if (hasCp && hasHp) {
            return ScreenContext(
                ScanScene.DETAILS, SceneConfidence.HIGH,
                "PC e PS encontrados na mesma captura"
            )
        }
        if ((hasCp || hasHp) && pokemonDetailsText.containsMatchIn(normalized)) {
            return ScreenContext(
                ScanScene.DETAILS, SceneConfidence.MEDIUM,
                "Dados parciais e rótulos de detalhes do Pokémon"
            )
        }
        return ScreenContext(
            ScanScene.OTHER, SceneConfidence.LOW,
            "Ainda não há informação suficiente para reconhecer esta tela"
        )
    }
}
