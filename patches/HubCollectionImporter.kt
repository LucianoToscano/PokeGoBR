package br.com.pokegobr

import android.content.Context
import android.net.Uri
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets

/**
 * PGSStats-style import: extract an allowlist of local fields only.
 * No account name, trainer ID, coordinates, tokens or inventory items are retained.
 * Import never sends or uploads content.
 */
internal object HubCollectionImporter {
    private const val MAX_BYTES = 8 * 1024 * 1024
    private const val MAX_POKEMON = 2000

    internal fun parseAndMinimize(json: String): JSONArray {
        val root = JSONObject(json)
        val pokemon = root.optJSONArray("pokemons")
            ?: root.optJSONObject("account")?.optJSONArray("pokemons")
            ?: throw IllegalArgumentException("Não foi encontrado o vetor 'pokemons'")
        val output = JSONArray()
        for (index in 0 until minOf(MAX_POKEMON, pokemon.length())) {
            val item = pokemon.optJSONObject(index) ?: continue
            val inner = item.optJSONObject("pokemon")
            val rawName = listOf(
                item.optString("name"),
                item.optString("pokemonName"),
                item.optString("speciesName"),
                inner?.optString("name").orEmpty(),
                inner?.optString("speciesName").orEmpty()
            ).firstOrNull { it.isNotBlank() }
            val id = item.optInt("pokemonId", -1).takeIf { it in 1..10000 }
                ?: item.optInt("speciesId", -1).takeIf { it in 1..10000 }
            val displayName = rawName?.take(65) ?: id?.let { "#$it" }
            if (displayName == null) continue

            val rawCp = listOf("cp", "pc", "combatPower").mapNotNull { key ->
                item.opt(key)?.toString()?.toIntOrNull()
            }.firstOrNull { it in 10..99999 }
            val attack = score(item, "individualAttack", "ivAttack", "attackIV")
            val defense = score(item, "individualDefense", "ivDefense", "defenseIV")
            val stamina = score(item, "individualStamina", "ivStamina", "staminaIV")
            val ivs = if (attack != null && defense != null && stamina != null)
                "$attack/$defense/$stamina" else "—"
            val minimized = JSONObject()
                .put("name", displayName)
                .put("cp", rawCp?.toString() ?: "—")
                .put("iv", ivs)
            output.put(minimized)
        }
        if (output.length() == 0) {
            throw IllegalArgumentException("Não há registros de Pokémon em campos compatíveis")
        }
        return output
    }

    private fun score(item: JSONObject, vararg keys: String): Int? =
        keys.mapNotNull { item.opt(it)?.toString()?.toIntOrNull() }
            .firstOrNull { it in 0..15 }

    fun importStats(context: Context, uri: Uri): Int {
        val input = context.contentResolver.openInputStream(uri)
            ?: throw IllegalArgumentException("Não foi possível abrir o documento")
        val buffer = ByteArrayOutputStream()
        input.use { source ->
            val chunk = ByteArray(16 * 1024)
            while (true) {
                val count = source.read(chunk)
                if (count < 0) break
                if (buffer.size() + count > MAX_BYTES)
                    throw IllegalArgumentException("Arquivo acima de 8 MB")
                buffer.write(chunk, 0, count)
            }
        }
        val minimized = parseAndMinimize(buffer.toString(StandardCharsets.UTF_8.name()))
        context.getSharedPreferences("pokegobr_imported_collection", Context.MODE_PRIVATE)
            .edit().putString("items", minimized.toString()).apply()
        return minimized.length()
    }
}
