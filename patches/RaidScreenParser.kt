package br.com.pokegobr

import java.text.Normalizer
import java.util.Locale

/**
 * Conservative reader for explicit raid fields in LOCAL ML Kit OCR text.
 * A raid lobby, raid boss or timer is never inferred from arbitrary screen text.
 * No network calls, booking, online room discovery or proprietary game datasets.
 */
internal data class RaidOcrSummary(
    val boss: String? = null,
    val timeRemaining: String? = null,
    val players: String? = null,
    val recognized: Boolean = false
) {
    fun display(): String {
        if (!recognized) return "Nenhuma tela de raide confirmada."
        val lines = arrayListOf("Raide reconhecida pelo texto da tela")
        if (boss != null) lines.add("Chefe (OCR, conferir): $boss")
        if (timeRemaining != null) lines.add("Tempo indicado: $timeRemaining")
        if (players != null) lines.add("Participantes indicados: $players")
        if (lines.size == 1) lines.add("Dados de chefe/tempo ainda não identificados.")
        lines.add("Verifique os dados no Pokémon GO.")
        return lines.joinToString("\n")
    }
}

internal object RaidScreenParser {
    private val raids = Regex(
        "\\b(?:CHEFE (?:DE |DA )?REIDE|BATALHA DE REIDE|ENTRAR NA REIDE|" +
            "RAID BOSS|RAID BATTLE|RAID LOBBY|RAID INVITE|CHEFE DA RAID)\\b"
    )

    private val boss = Regex(
        "(?:CHEFE (?:DE |DA )?REIDE|RAID BOSS)\\s*[:\\-]\\s*" +
            "([\\p{L}][\\p{L} .\\-']{1,33})"
    )
    private val time = Regex(
        "(?:TEMPO RESTANTE|REMAINING TIME|TIME LEFT|TEMPO ATE A REIDE|" +
            "CONTAGEM REGRESSIVA)\\s*[:\\-]?\\s*" +
            "(\\d{1,2}):(\\d{2})\\b"
    )
    private val players = Regex(
        "(?:TREINADORES|JOGADORES|TRAINERS|PLAYERS|PARTICIPANTES)\\s*[:\\-]?\\s*" +
            "(\\d{1,2})\\s*/\\s*(\\d{1,2})\\b"
    )

    private fun clean(text: String): String =
        Normalizer.normalize(text, Normalizer.Form.NFD)
            .replace(Regex("\\p{M}+"), "")
            .uppercase(Locale.ROOT)

    fun parse(text: String): RaidOcrSummary {
        val normalized = clean(text)
        if (!raids.containsMatchIn(normalized)) return RaidOcrSummary()

        // Only capture fields explicitly following an OCR label. Unrelated clock
        // time, CP, trainer names, and Pokémon names remain unknown.
        val bossName = boss.find(normalized)?.groupValues?.getOrNull(1)
            ?.trim()?.replace(Regex("\\s+"), " ")?.takeIf {
                it.length in 2..35 && !it.startsWith("TEMPO") &&
                !it.startsWith("TIME") && !it.startsWith("TREINADORES")
            }
        val timeRemaining = time.find(normalized)?.let { match ->
            val mins = match.groupValues[1].toIntOrNull()
            val sec = match.groupValues[2].toIntOrNull()
            if (mins != null && sec != null && mins in 0..99 && sec in 0..59)
                String.format(Locale.ROOT, "%02d:%02d", mins, sec)
            else null
        }
        val participants = players.find(normalized)?.let { match ->
            val current = match.groupValues[1].toIntOrNull()
            val capacity = match.groupValues[2].toIntOrNull()
            if (current != null && capacity != null &&
                current in 0..20 && capacity in 1..20 && current <= capacity) {
                "$current/$capacity"
            } else null
        }
        return RaidOcrSummary(
            boss = bossName, timeRemaining = timeRemaining,
            players = participants, recognized = true
        )
    }
}
