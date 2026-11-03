package br.com.pokegobr

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.text.Normalizer
import java.util.Locale
import java.util.UUID
import kotlin.math.roundToInt

internal data class SavedScan(
    val key: String, val bucket: String, val source: String,
    val index: Int, val signature: String, val name: String,
    val number: Int?, val cp: Int?, val hp: Int?, val iv: List<Int>?,
    val gl: String?, val ul: String?, val little: String?,
    val date: Long?, val verified: Boolean, val edited: Boolean,
    val imagePath: String? = null
) {
    val percent: Int? get() = iv?.sum()?.let { (it * 100.0 / 45.0).roundToInt() }
    val stars: Int? get() = iv?.sum()?.let {
        when {
            it == 45 -> 4
            it >= 37 -> 3
            it >= 30 -> 2
            it >= 23 -> 1
            else -> 0
        }
    }
    val own: Boolean get() = bucket == "scanner"
    val hasPvp: Boolean get() = gl != null || ul != null || little != null
}

internal enum class ScanFilter(val label: String) {
    ALL("Todos"), OWN("Scans"), IMPORTS("Importados"), VERIFIED("Conferidos"),
    ESTIMATES("Estimados"), PERFECT("100%"), PVP("PvP")
}
internal enum class ScanSort(val label: String) {
    DATE("Data"), NAME("Nome"), IV("IV geral"), CP("PC"), HP("PS"),
    GL("Rank GL"), UL("Rank UL")
}

internal object ScanHistoryStore {
    private const val SCAN_PREF = "pokegobr_scans"
    private const val IMPORT_PREF = "pokegobr_imported_collection"
    private val idPattern = Regex("[a-zA-Z0-9-]{10,80}")

    private fun number(any: Any?, min: Int, max: Int): Int? =
        any?.toString()?.trim()?.toIntOrNull()?.takeIf { it in min..max }

    fun parse(array: JSONArray, bucket: String, source: String,
              images: Map<String, String> = emptyMap()): List<SavedScan> {
        val result = ArrayList<SavedScan>(array.length())
        for (i in 0 until array.length()) {
            val row = array.optJSONObject(i) ?: continue
            val parts = row.optString("iv").replace(" ", "").split("/")
            val values = if (parts.size == 3) parts.mapNotNull {
                number(it, 0, 15)
            }.takeIf { it.size == 3 } else null
            val key = if (bucket == "scanner") {
                row.optString("historyId").takeIf { idPattern.matches(it) }
                    ?: "legacy-$i"
            } else bucket + "-" + i
            val rank = { field: String ->
                row.optString(field).trim().takeIf { it.isNotEmpty() && it != "—" }
                    ?.take(30)
            }
            result.add(SavedScan(key, bucket, source, i, row.toString(),
                row.optString("name").trim().ifBlank { "Pokémon" }.take(70),
                number(row.opt("speciesId"), 1, 10000),
                number(row.opt("cp"), 10, 99999),
                number(row.opt("hp"), 1, 9999), values,
                rank("glRank"), rank("ulRank"), rank("llRank"),
                row.optLong("createdAt", 0L)
                    .takeIf { it in 946684800000L..4102444800000L },
                row.optBoolean("confirmed", false),
                row.optBoolean("manuallyEdited", false),
                if (bucket == "scanner") images[key] else null
            ))
        }
        return result
    }

    private fun array(context: Context, pref: String, key: String): JSONArray =
        runCatching { JSONArray(context.getSharedPreferences(pref, Context.MODE_PRIVATE)
            .getString(key, "[]") ?: "[]") }.getOrDefault(JSONArray())

    private fun scanner(context: Context): JSONArray {
        val records = array(context, SCAN_PREF, "items")
        var changed = false
        for (i in 0 until records.length()) {
            val entry = records.optJSONObject(i) ?: continue
            if (!idPattern.matches(entry.optString("historyId"))) {
                entry.put("historyId", UUID.randomUUID().toString())
                changed = true
            }
        }
        if (changed) context.getSharedPreferences(SCAN_PREF, Context.MODE_PRIVATE)
            .edit().putString("items", records.toString()).apply()
        return records
    }

    fun load(context: Context): List<SavedScan> {
        val output = mutableListOf<SavedScan>()
        output += parse(scanner(context), "scanner", "Scanner",
            ScanPhotoStore.currentPhotos(context))
        output += parse(array(context, IMPORT_PREF, "items"), "legacy",
            "Importação anterior")
        for ((key, label) in HubImportV15.sources) {
            output += parse(array(context, IMPORT_PREF, key), key, label)
        }
        return output
    }

    private fun folded(text: String): String =
        Normalizer.normalize(text, Normalizer.Form.NFD)
            .replace(Regex("""\p{M}+"""), "")
            .lowercase(Locale.ROOT)
    private fun rank(text: String?): Long? =
        text?.substringBefore('–')?.substringBefore('-')?.trim()?.toLongOrNull()

    fun select(data: List<SavedScan>, search: String, filter: ScanFilter,
               sort: ScanSort, descending: Boolean): List<SavedScan> {
        val q = folded(search.trim()).removePrefix("#")
        return data.filter { row ->
            (q.isEmpty() || folded(row.name).contains(q) ||
                row.number?.toString() == q || row.cp?.toString() == q ||
                row.iv?.joinToString("/") == q) &&
                when (filter) {
                    ScanFilter.ALL -> true
                    ScanFilter.OWN -> row.own
                    ScanFilter.IMPORTS -> !row.own
                    ScanFilter.VERIFIED -> row.own && row.verified
                    ScanFilter.ESTIMATES -> row.own && !row.verified
                    ScanFilter.PERFECT -> row.percent == 100
                    ScanFilter.PVP -> row.hasPvp
                }
        }.sortedWith { a, b ->
            fun compare(x: Long?, y: Long?): Int = when {
                x == null && y != null -> 1
                x != null && y == null -> -1
                x == null -> 0
                else -> x.compareTo(y!!) * (if (descending) -1 else 1)
            }
            val result = when (sort) {
                ScanSort.DATE -> compare(a.date, b.date)
                ScanSort.NAME -> folded(a.name).compareTo(folded(b.name)) *
                    (if (descending) -1 else 1)
                ScanSort.IV -> compare(a.percent?.toLong(), b.percent?.toLong())
                ScanSort.CP -> compare(a.cp?.toLong(), b.cp?.toLong())
                ScanSort.HP -> compare(a.hp?.toLong(), b.hp?.toLong())
                ScanSort.GL -> compare(rank(a.gl), rank(b.gl))
                ScanSort.UL -> compare(rank(a.ul), rank(b.ul))
            }
            if (result != 0) result else a.key.compareTo(b.key)
        }
    }

    fun update(context: Context, scan: SavedScan, values: List<Int>,
               checked: Boolean): Boolean {
        if (!scan.own || values.size != 3 || values.any { it !in 0..15 }) return false
        val rows = scanner(context)
        for (i in 0 until rows.length()) {
            val row = rows.optJSONObject(i) ?: continue
            if (row.optString("historyId") != scan.key) continue
            val value = values.joinToString("/")
            val changed = row.optString("iv") != value
            row.put("iv", value)
            row.put("confirmed", checked)
            row.put("manuallyEdited", row.optBoolean("manuallyEdited", false) || changed)
            row.put("origin", if (checked) "scanner_user_reviewed" else "scanner_estimate")
            context.getSharedPreferences(SCAN_PREF, Context.MODE_PRIVATE)
                .edit().putString("items", rows.toString()).apply()
            return true
        }
        return false
    }

    fun copyImported(context: Context, scan: SavedScan): Boolean {
        if (scan.own) return false
        val rows = scanner(context)
        if (rows.length() >= 1000) return false
        val item = JSONObject()
            .put("historyId", UUID.randomUUID().toString())
            .put("name", scan.name)
            .put("cp", scan.cp?.toString() ?: "—")
            .put("hp", scan.hp?.toString() ?: "—")
            .put("iv", scan.iv?.joinToString("/") ?: "—")
            .put("createdAt", System.currentTimeMillis())
            .put("confirmed", false)
            .put("origin", "copy_from_" + scan.bucket)
        scan.number?.let { item.put("speciesId", it) }
        val next = JSONArray().put(item)
        for (i in 0 until rows.length()) next.put(rows.get(i))
        context.getSharedPreferences(SCAN_PREF, Context.MODE_PRIVATE)
            .edit().putString("items", next.toString()).apply()
        return true
    }

    fun delete(context: Context, scan: SavedScan): Boolean {
        val pref = if (scan.own) SCAN_PREF else IMPORT_PREF
        val key = if (scan.own || scan.bucket == "legacy") "items" else scan.bucket
        val rows = array(context, pref, key)
        if (rows.optJSONObject(scan.index)?.toString() != scan.signature) return false
        val next = JSONArray()
        for (i in 0 until rows.length()) if (i != scan.index) next.put(rows.get(i))
        context.getSharedPreferences(pref, Context.MODE_PRIVATE)
            .edit().putString(key, next.toString()).apply()
        if (scan.own) ScanPhotoStore.delete(context, scan.key)
        return true
    }

    fun exportCsv(records: List<SavedScan>): String {
        fun quoted(value: String): String {
            val clear = value.replace("\r", " ").replace("\n", " ")
            val safe = if (clear.firstOrNull() in listOf('=', '+', '-', '@'))
                "'" + clear else clear
            return "\"" + safe.replace("\"", "\"\"") + "\""
        }
        val output = mutableListOf(
            listOf("Fonte","Nome","Numero","PC","PS","Ataque","Defesa","PS_IV",
                "IV_percentual","Estrelas","Confirmado","Data_ms","Rank_GL",
                "Rank_UL","Rank_LL").joinToString(",") { quoted(it) }
        )
        for (item in records) {
            val values = listOf(item.source,item.name,item.number?.toString().orEmpty(),
                item.cp?.toString().orEmpty(),item.hp?.toString().orEmpty(),
                item.iv?.get(0)?.toString().orEmpty(),
                item.iv?.get(1)?.toString().orEmpty(),
                item.iv?.get(2)?.toString().orEmpty(),
                item.percent?.toString().orEmpty(), item.stars?.toString().orEmpty(),
                item.verified.toString(),item.date?.toString().orEmpty(),
                item.gl.orEmpty(),item.ul.orEmpty(),item.little.orEmpty())
            output.add(values.joinToString(",") { quoted(it) })
        }
        return output.joinToString("\r\n") + "\r\n"
    }
}
