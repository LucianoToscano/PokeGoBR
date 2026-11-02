package br.com.pokegobr

import android.content.Context
import android.net.Uri
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.util.Locale
import java.util.zip.ZipInputStream

/** Privacy-first import of user-selected Pokémon GO exports. */
internal data class ImportedBatch(val key: String, val label: String, val records: JSONArray) {
    val count: Int get() = records.length()
    val recognizedIvs: Int get() = (0 until count).count { records.optJSONObject(it)?.optString("iv") != "—" }
    val ranked: Int get() = (0 until count).count {
        val r = records.optJSONObject(it)
        r?.has("glRank") == true || r?.has("ulRank") == true || r?.has("llRank") == true
    }
}

internal object HubImportV15 {
    private const val MAX_RAW_BYTES = 12 * 1024 * 1024
    private const val MAX_ROWS = 6000
    private const val PREFS = "pokegobr_imported_collection"
    val sources = listOf(
        "pgsstats" to "PGSStats",
        "pokegenie" to "Poke Genie",
        "calcy" to "Calcy IV"
    )

    private fun readBounded(stream: InputStream): ByteArray {
        val out = ByteArrayOutputStream()
        val buffer = ByteArray(16384)
        while (true) {
            val count = stream.read(buffer)
            if (count < 0) return out.toByteArray()
            if (out.size() + count > MAX_RAW_BYTES)
                throw IllegalArgumentException("Arquivo acima do limite de 12 MB")
            out.write(buffer, 0, count)
        }
    }

    fun readAll(context: Context, uri: Uri): ImportedBatch {
        val bytes = context.contentResolver.openInputStream(uri)?.use(::readBounded)
            ?: throw IllegalArgumentException("Não foi possível abrir o arquivo")
        val dex = HubData.cachedSpecies(context).associate { it.number to it.name }
        val batch = parse(bytes, dex)
        // Only the minimized fields are stored; raw export bytes do not persist.
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(batch.key, batch.records.toString()).apply()
        return batch
    }

    fun previous(context: Context, key: String): JSONArray {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return runCatching { JSONArray(prefs.getString(key, "[]") ?: "[]") }
            .getOrDefault(JSONArray())
    }

    fun parse(bytes: ByteArray, species: Map<Int, String> = emptyMap()): ImportedBatch {
        if (bytes.size > MAX_RAW_BYTES) throw IllegalArgumentException("Arquivo muito grande")
        if (bytes.size >= 4 && bytes[0] == 0xAC.toByte() &&
            bytes[1] == 0xED.toByte() && bytes[2] == 0x00.toByte() &&
            bytes[3] == 0x05.toByte())
            throw IllegalArgumentException(
                "PGSData .dat usa serialização Java: importação segura ainda não implementada. " +
                    "Use PGSStats.json ou PGSStats.zip."
            )
        val input = if (bytes.size >= 4 && bytes[0] == 0x50.toByte() &&
            bytes[1] == 0x4B.toByte() && bytes[2] == 0x03.toByte() &&
            bytes[3] == 0x04.toByte()) {
            ZipInputStream(ByteArrayInputStream(bytes)).use { zip ->
                var json: ByteArray? = null
                var count = 0
                while (true) {
                    val entry = zip.nextEntry ?: break
                    count++
                    if (count > 20) throw IllegalArgumentException("ZIP contém entradas demais")
                    val file = entry.name.replace('\\', '/').substringAfterLast('/')
                    if (!entry.isDirectory && file.equals("PGSStats.json", ignoreCase = true)) {
                        if (json != null) throw IllegalArgumentException("ZIP com PGSStats duplicado")
                        json = readBounded(zip)
                    }
                    zip.closeEntry()
                }
                json ?: throw IllegalArgumentException("ZIP sem arquivo PGSStats.json")
            }
        } else bytes
        val text = String(input, Charsets.UTF_8).removePrefix("\uFEFF").trimStart()
        return when {
            text.startsWith("{") -> ImportedBatch("pgsstats", "PGSStats", parsePgs(text, species))
            else -> parseCsv(text, species)
        }
    }

    private fun parsePgs(text: String, dex: Map<Int, String>): JSONArray {
        val root = JSONObject(text)
        val rows = root.optJSONArray("pokemons")
            ?: throw IllegalArgumentException("JSON sem a lista pokemons")
        val out = JSONArray()
        for (i in 0 until minOf(MAX_ROWS, rows.length())) {
            val row = rows.optJSONObject(i) ?: continue
            if (row.optBoolean("isEgg", false)) continue
            val id = row.optInt("pokemonId", row.optInt("speciesId", -1))
                .takeIf { it in 1..10000 }
            val speciesName = row.optString("speciesName").trim().takeIf {
                it.isNotBlank() && it.length <= 55
            }
            if (id == null && speciesName == null) continue
            val name = id?.let { dex[it] } ?: speciesName ?: "#$id"
            val attack = row.optInt("individualAttack", -1).takeIf { it in 0..15 }
            val defense = row.optInt("individualDefense", -1).takeIf { it in 0..15 }
            val stamina = row.optInt("individualStamina", -1).takeIf { it in 0..15 }
            val iv = if (attack != null && defense != null && stamina != null)
                "$attack/$defense/$stamina" else "—"
            val item = JSONObject()
                .put("name", name)
                .put("cp", cp(row.opt("cp")?.toString().orEmpty()))
                .put("iv", iv)
            if (id != null) item.put("speciesId", id)
            out.put(item)
        }
        if (out.length() == 0)
            throw IllegalArgumentException("Nenhum Pokémon válido no PGSStats")
        return out
    }

    private fun iv(raw: String): Int? {
        val n = raw.trim().toDoubleOrNull() ?: return null
        if (n < 0.0 || n > 15.0 || n != n.toInt().toDouble()) return null
        return n.toInt()
    }

    private fun rank(raw: String): Int? =
        raw.trim().toIntOrNull()?.takeIf { it in 1..10000 }

    private fun cp(raw: String): String =
        raw.trim().toIntOrNull()?.takeIf { it in 10..99999 }?.toString() ?: "—"

    private fun parseCsv(text: String, dex: Map<Int, String>): ImportedBatch {
        val rows = CsvTableV15.read(text)
        if (rows.size < 2) throw IllegalArgumentException("CSV sem registros")
        val header = rows[0].map { it.removePrefix("\uFEFF").trim() }
        val indices = header.withIndex().associate { it.value to it.index }
        val genie = "Atq IV" in indices && "Pokemon" in indices && "Class # (G)" in indices
        val calcy = "ØATT IV" in indices && "Nr" in indices && "GL Rank (min)" in indices
        if (!genie && !calcy)
            throw IllegalArgumentException("CSV incompatível. Escolha exportação Poke Genie ou histórico Calcy IV")
        val result = JSONArray()
        for (row in rows.drop(1).take(MAX_ROWS)) {
            fun cell(label: String): String {
                val index = indices[label] ?: return ""
                return row.getOrNull(index)?.trim().orEmpty()
            }
            val id = (if (genie) cell("Pokemon") else cell("Nr"))
                .toIntOrNull()?.takeIf { it in 1..10000 }
            if (id == null) continue
            val name = dex[id] ?: (if (genie) cell("Nome") else cell("Name"))
                .takeIf { it.isNotBlank() && it.length <= 55 &&
                    it !in listOf("?", " - ", "-") }
            val item = JSONObject()
                .put("name", name ?: "#$id")
                .put("speciesId", id)
                .put("cp", cp(if (genie) cell("PC") else cell("CP")))
            val attack = iv(if (genie) cell("Atq IV") else cell("ØATT IV"))
            val defense = iv(if (genie) cell("Def IV") else cell("ØDEF IV"))
            val stamina = iv(if (genie) cell("PS IV") else cell("ØHP IV"))
            item.put("iv", if (attack != null && defense != null && stamina != null)
                "$attack/$defense/$stamina" else "—")

            if (genie) {
                rank(cell("Class # (G)"))?.let { item.put("glRank", it.toString()) }
                rank(cell("Class # (U)"))?.let { item.put("ulRank", it.toString()) }
                rank(cell("Class # (L)"))?.let { item.put("llRank", it.toString()) }
            } else {
                for ((short, key) in listOf("gl" to "GL", "ul" to "UL", "ll" to "LL")) {
                    val minimum = rank(cell("$key Rank (min)"))
                    val maximum = rank(cell("$key Rank (max)"))
                    if (minimum != null && maximum != null)
                        item.put(short + "Rank", if (minimum == maximum) minimum.toString()
                            else "$minimum–$maximum")
                }
            }
            result.put(item)
        }
        if (result.length() == 0)
            throw IllegalArgumentException("Nenhum Pokémon reconhecido nas colunas do CSV")
        return ImportedBatch(
            if (genie) "pokegenie" else "calcy",
            if (genie) "Poke Genie" else "Calcy IV",
            result
        )
    }
}

/** Bounded, quoted CSV reader; commas and newlines inside quotes are handled. */
internal object CsvTableV15 {
    fun read(text: String): List<List<String>> {
        if (text.length > 12 * 1024 * 1024)
            throw IllegalArgumentException("CSV acima de 12 MB")
        val all = ArrayList<List<String>>()
        val row = ArrayList<String>()
        val field = StringBuilder()
        var quoted = false
        var i = 0
        fun addField() {
            if (field.length > 2048) throw IllegalArgumentException("Campo CSV longo demais")
            if (row.size >= 100) throw IllegalArgumentException("CSV com colunas demais")
            row.add(field.toString())
            field.setLength(0)
        }
        fun addRow() {
            addField()
            if (row.any { it.isNotBlank() }) all.add(ArrayList(row))
            row.clear()
            if (all.size > 6001) throw IllegalArgumentException("CSV com registros demais")
        }
        while (i < text.length) {
            val ch = text[i]
            when {
                ch == '"' && quoted && i + 1 < text.length && text[i + 1] == '"' -> {
                    field.append('"'); i++
                }
                ch == '"' -> quoted = !quoted
                !quoted && ch == ',' -> addField()
                !quoted && ch == '\n' -> addRow()
                !quoted && ch == '\r' -> {
                    addRow()
                    if (i + 1 < text.length && text[i + 1] == '\n') i++
                }
                else -> {
                    field.append(ch)
                    if (field.length > 2048) throw IllegalArgumentException("Campo CSV longo demais")
                }
            }
            i++
        }
        if (quoted) throw IllegalArgumentException("CSV com aspas não fechadas")
        if (field.isNotEmpty() || row.isNotEmpty()) addRow()
        return all
    }
}
