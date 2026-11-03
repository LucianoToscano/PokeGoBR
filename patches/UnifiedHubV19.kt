package br.com.pokegobr

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.text.Normalizer
import java.util.Locale
import kotlin.math.roundToInt

private val hubText = Color(0xFFEDF6FA)
private val hubAccent = Color(0xFF9AE4D1)
private val hubPanel = Color(0xD9203035)

internal data class DexSpecies(val number: Int, val name: String)
internal data class ReferenceEntry(val name: String, val format: String, val priority: String, val area: String)

/**
 * Starter data is a hand-authored index, not species statistics or a GO Game
 * Master. PokeAPI supplies general Pokémon taxonomy only, on user request.
 */
internal object HubData {
    val starterDex = listOf(
        DexSpecies(1, "Bulbasaur"), DexSpecies(4, "Charmander"),
        DexSpecies(7, "Squirtle"), DexSpecies(25, "Pikachu"),
        DexSpecies(92, "Gastly"), DexSpecies(93, "Haunter"),
        DexSpecies(94, "Gengar"), DexSpecies(149, "Dragonite"),
        DexSpecies(150, "Mewtwo"), DexSpecies(252, "Treecko"),
        DexSpecies(253, "Grovyle"), DexSpecies(254, "Sceptile"),
        DexSpecies(656, "Froakie"),
        DexSpecies(657, "Frogadier"), DexSpecies(658, "Greninja"),
        DexSpecies(888, "Zacian"), DexSpecies(890, "Eternatus"),
        DexSpecies(1025, "Pecharunt")
    )

    fun cachedSpecies(context: Context): List<DexSpecies> = runCatching {
        val text = context.openFileInput("species_index_v1.json").bufferedReader().use { it.readText() }
        parseSpecies(text)
    }.getOrDefault(starterDex)

    internal fun parseSpecies(text: String): List<DexSpecies> {
        val array = JSONObject(text).getJSONArray("results")
        val result = ArrayList<DexSpecies>(array.length())
        for (i in 0 until array.length()) {
            val item = array.optJSONObject(i) ?: continue
            val rawId = item.optString("url").trimEnd('/').substringAfterLast('/').toIntOrNull()
            val name = item.optString("name").trim()
            if (rawId != null && rawId in 1..10000 && name.isNotBlank() &&
                name.length <= 80) result.add(DexSpecies(rawId, name))
        }
        if (result.isEmpty()) error("Catálogo sem espécies válidas")
        return result.distinctBy { it.number }.sortedBy { it.number }
    }

    suspend fun refreshSpecies(context: Context): List<DexSpecies> = withContext(Dispatchers.IO) {
        val conn = URL("https://pokeapi.co/api/v2/pokemon-species?limit=2000")
            .openConnection() as HttpURLConnection
        conn.connectTimeout = 10000
        conn.readTimeout = 12000
        try {
            if (conn.responseCode != 200) error("HTTP " + conn.responseCode)
            val response = conn.inputStream.bufferedReader().use { it.readText() }
            if (response.length > 600_000) error("Resposta excessivamente grande")
            val parsed = parseSpecies(response)
            context.openFileOutput("species_index_v1.json", Context.MODE_PRIVATE)
                .bufferedWriter().use { it.write(response) }
            parsed
        } finally {
            conn.disconnect()
        }
    }

    fun references(context: Context): List<ReferenceEntry> = runCatching {
        val json = context.assets.open("pokegobr_reference_catalog.json")
            .bufferedReader().use { it.readText() }
        val arr = JSONObject(json).getJSONArray("records")
        (0 until arr.length()).mapNotNull { index ->
            arr.optJSONObject(index)?.let { item ->
                ReferenceEntry(
                    item.optString("name"), item.optString("format"),
                    item.optString("priority"), item.optString("area")
                )
            }
        }
    }.getOrDefault(emptyList())
}

internal fun normalizeDexQuery(text: String): String =
    Normalizer.normalize(text, Normalizer.Form.NFD).replace(Regex("\\p{M}+"), "")
        .lowercase(Locale.ROOT).trim()

@Composable
private fun HubBox(heading: String, content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier.fillMaxWidth()
            .background(hubPanel, RoundedCornerShape(16.dp))
            .border(0.8.dp, Color.White.copy(alpha = .19f), RoundedCornerShape(16.dp))
            .padding(13.dp),
        verticalArrangement = Arrangement.spacedBy(9.dp)
    ) {
        Text(heading, fontSize = 17.sp, fontWeight = FontWeight.Bold, color = hubAccent)
        content()
    }
}

@Composable
private fun HubLabel(value: String) {
    Text(value, fontSize = 12.sp, color = hubText, lineHeight = 17.sp)
}

@Composable
internal fun UnifiedModuleScreen(module: String, onNavigate: (Int) -> Unit, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth().verticalScroll(rememberScrollState())
        .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(11.dp)) {
        when (module) {
            "Pokédex" -> HubPokedex()
            "Meus Pokémon" -> HubCollection(onNavigate)
            "PvP" -> HubPvP(onNavigate)
            "Calculadoras" -> HubCalculator(onNavigate)
            "Trocas" -> HubNotepad("Lista pessoal de trocas", "pokegobr_trades",
                "Anote Pokémon para oferecer ou procurar. Sem leilão e sem envio automático.")
            "Itens" -> HubNotepad("Planejamento de itens", "pokegobr_items",
                "Anotações locais. Não lê automaticamente a mochila do Pokémon GO.")
            "Comunidade" -> HubCommunity()
            "Extras" -> HubExtras()
            else -> HubBox(module) {
                HubLabel("O scanner de Pokémon e o scanner de raides continuam disponíveis na navegação.")
                OutlinedButton(onClick = { onNavigate(8) }) { Text("Abrir scanner") }
            }
        }
        Spacer(Modifier.height(18.dp))
    }
}

@Composable
private fun HubPokedex() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var pokemon by remember { mutableStateOf(HubData.cachedSpecies(context)) }
    var search by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    val query = normalizeDexQuery(search)
    val matches = remember(pokemon, query) {
        pokemon.filter {
            query.isEmpty() || normalizeDexQuery(it.name).contains(query) ||
                it.number.toString() == query.removePrefix("#")
        }.take(45)
    }
    HubBox("Pokédex — catálogo unificado") {
        OutlinedButton(onClick = { openGoDex(context, GoDexLinks.home) }) {
            Text("GoDex · coleções e variantes", fontSize = 12.sp)
        }
        HubLabel("Lista de espécies, pesquisa por nome/número e cache local. A fonte externa PokéAPI é taxonômica: NÃO representa atributos, golpes ou formas específicos do Pokémon GO.")
        OutlinedTextField(
            value = search, onValueChange = { search = it.take(45) },
            label = { Text("Espécie ou número") }, singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            HubLabel("Espécies no índice: " + pokemon.size)
            Spacer(Modifier.weight(1f))
            TextButton(enabled = !busy, onClick = {
                busy = true
                status = "Atualizando catálogo público..."
                scope.launch {
                    try {
                        pokemon = HubData.refreshSpecies(context)
                        status = "Catálogo atualizado. Disponível offline neste aparelho."
                    } catch (ex: Exception) {
                        status = "Sem conexão ou falha na fonte: " +
                            (ex.message ?: "erro desconhecido") + ". Índice anterior preservado."
                    } finally { busy = false }
                }
            }) { Text(if (busy) "Atualizando..." else "Atualizar índice", fontSize = 12.sp) }
        }
        if (status.isNotEmpty()) HubLabel(status)
        matches.forEach { species ->
            Text("#" + species.number + " · " +
                species.name.replaceFirstChar { it.uppercase() },
                color = hubText, fontSize = 13.sp)
        }
        if (matches.isEmpty()) HubLabel("Nenhuma espécie no índice atual.")
        if (pokemon.size > matches.size && query.isBlank()) {
            HubLabel("Mostrando as primeiras 45 espécies. Use a pesquisa para localizar outras.")
        }
        HubLabel("Tipos, IVs, formas, golpes e estatísticas GO: dependem de um Game Master versionado; ainda não são calculados nesta tela.")
    }
}

@Composable
private fun HubCollection(onNavigate: (Int) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var version by remember { mutableIntStateOf(0) }
    var status by remember { mutableStateOf("") }
    var filter by remember { mutableStateOf("") }
    val scannerItems = remember(version) {
        runCatching {
            JSONArray(context.getSharedPreferences("pokegobr_scans", Context.MODE_PRIVATE)
                .getString("items", "[]"))
        }.getOrDefault(JSONArray())
    }
    val legacy = remember(version) {
        runCatching {
            JSONArray(context.getSharedPreferences("pokegobr_imported_collection", Context.MODE_PRIVATE)
                .getString("items", "[]"))
        }.getOrDefault(JSONArray())
    }
    val imported = remember(version) {
        HubImportV15.sources.map { (key, label) ->
            label to HubImportV15.previous(context, key)
        }
    }
    val totalImported = legacy.length() + imported.sumOf { it.second.length() }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            status = "Verificando formato e importando no aparelho..."
            scope.launch {
                status = try {
                    val batch = withContext(Dispatchers.IO) {
                        HubImportV15.readAll(context, uri)
                    }
                    version++
                    "${batch.label}: ${batch.count} registros; " +
                        "${batch.recognizedIvs} com IV; ${batch.ranked} com ranking PvP. " +
                        "Dados somente neste aparelho."
                } catch (ex: Exception) {
                    "Importação não realizada: " + (ex.message ?: "formato desconhecido")
                }
            }
        }
    }
    HubBox("Meus Pokémon — importações locais") {
        OutlinedButton(onClick = { openGoDex(context, GoDexLinks.home) }) {
            Text("Abrir minhas coleções no GoDex", fontSize = 12.sp)
        }
        HubLabel("Scanner: ${scannerItems.length()} · Exportações: $totalImported registros. " +
            "Um mesmo Pokémon pode aparecer em mais de uma origem; registros não são indivíduos únicos.")
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            OutlinedButton(onClick = { onNavigate(8) }) {
                Text("Scanner", fontSize = 12.sp)
            }
            OutlinedButton(onClick = {
                picker.launch(arrayOf(
                    "text/csv", "text/plain", "application/json", "application/zip",
                    "application/x-zip-compressed", "application/octet-stream", "*/*"
                ))
            }) { Text("Importar CSV/ZIP/JSON", fontSize = 12.sp) }
        }
        HubLabel("Compatível: Poke Genie CSV, Calcy IV histórico CSV, PGSStats.json e PGSStats.zip. " +
            "PGSData.dat (serialização Java) ainda não pode ser importado com segurança.")
        if (status.isNotBlank()) HubLabel(status)
        HubLabel("Os CSVs podem incluir ranks GL, UL e LL: são valores históricos da fonte, " +
            "não rankings recalculados ou confirmados como atuais.")
        for ((label, rows) in imported) {
            HubLabel("$label: ${rows.length()} registros salvos")
        }
        if (legacy.length() > 0) HubLabel("Importação anterior: ${legacy.length()}")
        OutlinedTextField(
            value = filter, onValueChange = { filter = it.take(60) },
            singleLine = true, label = { Text("Pesquisar nome ou número") },
            modifier = Modifier.fillMaxWidth()
        )
        val query = normalizeDexQuery(filter)
        var shown = 0
        for ((source, records) in listOf("Scanner" to scannerItems,
            "Importação anterior" to legacy) + imported) {
            for (i in 0 until records.length()) {
                if (shown >= 24) break
                val item = records.optJSONObject(i) ?: continue
                val name = item.optString("name", "Pokémon")
                val speciesId = item.optInt("speciesId", 0)
                if (query.isNotBlank() && !normalizeDexQuery(name).contains(query) &&
                    speciesId.toString() != query.removePrefix("#")) continue
                val extras = listOf(
                    "GL" to "glRank", "UL" to "ulRank", "LL" to "llRank"
                ).mapNotNull { (league, key) ->
                    item.optString(key).takeIf { it.isNotBlank() }?.let {
                        "$league #$it"
                    }
                }.joinToString(" · ")
                HubLabel("$source · $name · PC ${item.optString("cp", "—")} · " +
                    "IV ${item.optString("iv", "—")}" +
                    if (extras.isNotBlank()) "\n$extras (fonte)" else "")
                shown++
            }
            if (shown >= 24) break
        }
        if (shown == 0) HubLabel("Nenhum registro encontrado. Importe um arquivo ou use o scanner.")
        if (shown >= 24) HubLabel("Mostrando 24 registros por vez. Refine a pesquisa.")
        HubLabel("Importação privada: salvamos apenas espécie, PC, IV e ranks informados nas fontes. " +
            "Não salvamos arquivo bruto, nickname, localização, código de treinador ou dados de conta.")
    }
}

@Composable
private fun HubPvP(onNavigate: (Int) -> Unit) {
    val context = LocalContext.current
    var league by remember { mutableStateOf("GL") }
    val importedRanks = remember(league) {
        val key = when (league) { "GL" -> "glRank"; "UL" -> "ulRank"; else -> "llRank" }
        HubImportV15.sources.filter { it.first != "pgsstats" }.flatMap { (source, name) ->
            val arr = HubImportV15.previous(context, source)
            (0 until arr.length()).mapNotNull { i ->
                arr.optJSONObject(i)?.takeIf { it.has(key) }?.let { row ->
                    Triple(name, row.optString("name", "Pokémon"),
                        row.optString(key))
                }
            }
        }.take(12)
    }
    HubBox("PvP — ranks importados") {
        HubLabel("Grande Liga (GL) até PC 1500 · Ultra-Liga (UL) até PC 2500 · " +
            "Liga Pequena (LL) conforme a origem dos CSVs. Liga Mestra exige dados próprios.")
        Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            listOf("GL", "UL", "LL").forEach { label ->
                TextButton(onClick = { league = label }) {
                    Text(label, color = if (league == label) hubAccent else hubText)
                }
            }
        }
        if (importedRanks.isEmpty()) {
            HubLabel("Ainda não há ranks $league importados.")
        } else {
            importedRanks.forEach { (source, name, rank) ->
                HubLabel("$name · $league #$rank · $source")
            }
        }
        HubLabel("Os ranks apresentados são valores informados pelo Poke Genie/Calcy IV " +
            "nas exportações e podem variar por espécie, evolução, forma e data. " +
            "Não são recalculados nem validados pelo PokeGoBR.")
        OutlinedButton(onClick = { onNavigate(8) }) { Text("Abrir scanner IV") }
        HubLabel("Motor próprio de PvP, ranks atuais por forma, batalhas e turnos ainda pendentes.")
    }
}

@Composable
private fun HubCalculator(onNavigate: (Int) -> Unit) {
    var atk by remember { mutableFloatStateOf(15f) }
    var def by remember { mutableFloatStateOf(15f) }
    var sta by remember { mutableFloatStateOf(15f) }
    val a = atk.roundToInt(); val d = def.roundToInt(); val s = sta.roundToInt()
    val sum = a + d + s
    val percentage = sum * 100f / 45f
    HubBox("Calculadora IV — resultado verificável") {
        listOf(Triple("Ataque", atk, { v: Float -> atk = v }),
            Triple("Defesa", def, { v: Float -> def = v }),
            Triple("PS", sta, { v: Float -> sta = v })).forEach { (name, value, onChange) ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(name, fontSize = 12.sp, color = hubText, modifier = Modifier.width(65.dp))
                Slider(
                    value = value, onValueChange = onChange, valueRange = 0f..15f,
                    steps = 14, modifier = Modifier.weight(1f)
                )
                Text(value.roundToInt().toString(), color = hubAccent, modifier = Modifier.width(27.dp))
            }
        }
        val stars = when {
            sum == 45 -> "4★"
            sum >= 37 -> "3★"
            sum >= 30 -> "2★"
            sum >= 23 -> "1★"
            else -> "0★"
        }
        Text(a.toString() + "/" + d + "/" + s + " · " +
            String.format(Locale.US, "%.1f", percentage) + "% · " + stars,
            fontSize = 21.sp, color = hubAccent, fontWeight = FontWeight.Bold)
        HubLabel("Fórmula: (Ataque + Defesa + PS) ÷ 45 × 100. Não é eficiência PvP.")
        OutlinedButton(onClick = { onNavigate(8) }) { Text("Ler IVs no scanner") }
        HubLabel("Simulador de fortalecimento e custos Stardust/EduHunter: aguarda dados e fórmulas GO versionados.")
    }
}

@Composable
private fun HubNotepad(title: String, prefName: String, message: String) {
    val context = LocalContext.current
    val prefs = remember(prefName) { context.getSharedPreferences(prefName, Context.MODE_PRIVATE) }
    var text by remember { mutableStateOf("") }
    var items by remember { mutableStateOf(prefs.getString("items", "") ?: "") }
    HubBox(title) {
        HubLabel(message)
        OutlinedTextField(
            value = text, onValueChange = { text = it.take(180) },
            label = { Text("Nova anotação") }, modifier = Modifier.fillMaxWidth()
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(enabled = text.isNotBlank(), onClick = {
                items = (text.trim() + "\n" + items).take(6000)
                prefs.edit().putString("items", items).apply()
                text = ""
            }) { Text("Adicionar") }
            OutlinedButton(onClick = {
                items = ""; prefs.edit().remove("items").apply()
            }) { Text("Limpar") }
        }
        if (items.isNotEmpty()) HubLabel(items)
        else HubLabel("Sua lista está vazia.")
    }
}

@Composable
private fun HubCommunity() {
    val context = LocalContext.current
    var message by remember { mutableStateOf("") }
    HubBox("Comunidade — compartilhamento voluntário") {
        HubLabel("Escreva uma mensagem e escolha o aplicativo para compartilhar. Não existe chat ao vivo nem conexão com grupo de WhatsApp nesta versão.")
        OutlinedTextField(
            value = message, onValueChange = { message = it.take(1400) },
            label = { Text("Mensagem") }, modifier = Modifier.fillMaxWidth(), minLines = 3
        )
        OutlinedButton(enabled = message.isNotBlank(), onClick = {
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, message)
            }
            context.startActivity(Intent.createChooser(intent, "Compartilhar via"))
        }) { Text("Compartilhar texto") }
    }
}

@Composable
private fun HubExtras() {
    var tab by remember { mutableStateOf("Coordenadas") }
    Row(horizontalArrangement = Arrangement.spacedBy(3.dp), modifier = Modifier.fillMaxWidth()) {
        listOf("Coordenadas", "GO Rocket", "GoDex", "Inventário").forEach { option ->
            val selected = option == tab
            TextButton(onClick = { tab = option }, modifier = Modifier.weight(1f)) {
                Text(option, color = if (selected) hubAccent else hubText, fontSize = 10.sp)
            }
        }
    }
    when (tab) {
        "Coordenadas" -> HubCoordinates()
        "GO Rocket" -> HubRocket()
        "GoDex" -> HubGoDex()
        else -> HubReferences()
    }
}

private fun openGoDex(context: Context, url: String) {
    // Fixed public URLs or explicitly validated collection URLs only.
    val trusted = url in listOf(GoDexLinks.home, GoDexLinks.shinyDex,
        GoDexLinks.maxDex) || GoDexLinks.validPublicCollection(url) != null
    if (!trusted) return
    try {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    } catch (_: Exception) {
        Toast.makeText(context, "Não foi possível abrir o GoDex neste aparelho",
            Toast.LENGTH_SHORT).show()
    }
}

@Composable
private fun HubGoDex() {
    val context = LocalContext.current
    var collectionUrl by remember { mutableStateOf("") }
    val acceptedUrl = GoDexLinks.validPublicCollection(collectionUrl)
    HubBox("GoDex — coleções Pokémon GO") {
        HubLabel("Acompanhe Pokémon que você já tem ou ainda procura, " +
            "com coleções para Shiny, Hundo, Sombroso, Mega, eventos, formas, " +
            "trajes e gênero. O site abre no navegador para preservar seu login.")
        OutlinedButton(onClick = { openGoDex(context, GoDexLinks.home) },
            modifier = Modifier.fillMaxWidth()) {
            Text("Abrir GoDex em português")
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            OutlinedButton(
                onClick = { openGoDex(context, GoDexLinks.shinyDex) },
                modifier = Modifier.weight(1f)
            ) { Text("ShinyDex", fontSize = 12.sp) }
            OutlinedButton(
                onClick = { openGoDex(context, GoDexLinks.maxDex) },
                modifier = Modifier.weight(1f)
            ) { Text("Dynamax / GMax", fontSize = 12.sp) }
        }
        HubLabel("Coleção pública: cole abaixo um link que comece com " +
            "https://godex.site/public-collection/ para abrir diretamente.")
        OutlinedTextField(
            value = collectionUrl, onValueChange = { collectionUrl = it.take(140) },
            label = { Text("Link da coleção pública") },
            singleLine = true, modifier = Modifier.fillMaxWidth()
        )
        OutlinedButton(
            enabled = acceptedUrl != null,
            onClick = { acceptedUrl?.let { openGoDex(context, it) } }
        ) { Text("Abrir coleção pública") }
        if (collectionUrl.isNotBlank() && acceptedUrl == null) {
            HubLabel("Link inválido ou não público: verifique o endereço do GoDex.")
        }
        HubLabel("Integração por links nesta versão. O aplicativo não lê sua " +
            "sessão, senhas ou coleções privadas; não envia fotos do scanner. " +
            "Sincronização automática depende de uma API autenticada e autorizada.")
    }
}

@Composable
private fun HubCoordinates() {
    val context = LocalContext.current
    var input by remember { mutableStateOf("") }
    val coordinates = remember(input) { GeoCoordinateParser.parse(input) }
    HubBox("Links e coordenadas — leitor local") {
        HubLabel("Cole latitude/longitude ou um link do Google Maps. Validamos os limites antes de copiar ou abrir o aplicativo de mapas. Não lemos a posição do GPS.")
        OutlinedTextField(value = input, onValueChange = { input = it.take(2000) },
            label = { Text("Ex.: -5.188, -37.344") }, minLines = 2,
            modifier = Modifier.fillMaxWidth())
        if (coordinates != null) {
            Text(coordinates.asText(), color = hubAccent, fontSize = 16.sp,
                fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE)
                        as android.content.ClipboardManager
                    clipboard.setPrimaryClip(android.content.ClipData.newPlainText(
                        "Coordenadas PokeGoBR", coordinates.asText()
                    ))
                }) { Text("Copiar") }
                OutlinedButton(onClick = {
                    val uri = Uri.parse("geo:0,0?q=" + coordinates.latitude + "," +
                        coordinates.longitude)
                    val intent = Intent(Intent.ACTION_VIEW, uri)
                    if (intent.resolveActivity(context.packageManager) != null) {
                        context.startActivity(intent)
                    }
                }) { Text("Abrir mapa") }
            }
        } else if (input.isNotBlank()) {
            HubLabel("Não reconheci uma coordenada válida. Confira a latitude (-90 a 90) e longitude (-180 a 180).")
        }
        HubLabel("Esta função não movimenta personagem, não faz teleporte e não acessa o estado interno de aplicativos de GPS.")
    }
}

@Composable
private fun HubRocket() {
    HubBox("Equipe GO Rocket — referência inicial") {
        HubLabel("Líderes conhecidos: Cliff, Arlo, Sierra. Giovanni é o chefe da Equipe GO Rocket.")
        HubLabel("A composição das equipes muda por evento. Não há lista atualizada de encontros nem recomendações de counters verificadas nesta versão.")
        HubLabel("Módulo preparado para dados GO Rocket com data, origem, equipe e validade, sem usar automação de jogo.")
    }
}

@Composable
private fun HubReferences() {
    val context = LocalContext.current
    val entries = remember { HubData.references(context) }
    var search by remember { mutableStateOf("") }
    val query = normalizeDexQuery(search)
    val matches = entries.filter {
        query.isEmpty() || normalizeDexQuery(it.name).contains(query) ||
            it.priority.lowercase(Locale.ROOT) == query ||
            normalizeDexQuery(it.area).contains(query)
    }
    HubBox("Arquivo Mestre — referências de integração") {
        HubLabel("Inventário histórico local com " + entries.size +
            " registros de referência. NÃO são bibliotecas instaladas, APIs disponíveis nem arquivos executáveis embutidos.")
        OutlinedTextField(value = search, onValueChange = { search = it.take(60) },
            label = { Text("Pesquisar referência ou prioridade P0–P3") }, singleLine = true,
            modifier = Modifier.fillMaxWidth())
        matches.take(40).forEach { ref ->
            HubLabel(ref.priority + " · " + ref.name + " · " + ref.area)
        }
        if (matches.size > 40) HubLabel("Mostrando 40 de " + matches.size +
            " registros. Refine a pesquisa.")
        if (entries.isEmpty()) HubLabel("Catálogo não encontrado no pacote desta versão.")
        HubLabel("Fonte: índice enviado pelo proprietário do projeto. Sem links privados do Drive, credenciais, APKs ou dados de conta.")
    }
}
