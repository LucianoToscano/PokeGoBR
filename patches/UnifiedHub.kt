package br.com.pokegobr

import android.content.Context
import android.content.Intent
import android.net.Uri
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
        DexSpecies(150, "Mewtwo"), DexSpecies(658, "Greninja"),
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
    val scannerItems = remember(version) {
        runCatching {
            JSONArray(context.getSharedPreferences("pokegobr_scans", Context.MODE_PRIVATE)
                .getString("items", "[]"))
        }.getOrDefault(JSONArray())
    }
    val imported = remember(version) {
        runCatching {
            JSONArray(context.getSharedPreferences("pokegobr_imported_collection", Context.MODE_PRIVATE)
                .getString("items", "[]"))
        }.getOrDefault(JSONArray())
    }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            status = "Lendo arquivo local..."
            scope.launch {
                status = try {
                    val count = withContext(Dispatchers.IO) {
                        HubCollectionImporter.importStats(context, uri)
                    }
                    version++
                    "Importados " + count + " registros com campos identificados. Dados apenas no aparelho."
                } catch (ex: Exception) {
                    "Falha na importação: " + (ex.message ?: "formato não suportado")
                }
            }
        }
    }
    HubBox("Meus Pokémon — coleção local") {
        HubLabel("Avaliações salvas: " + scannerItems.length() +
            " · Pokémon importados: " + imported.length())
        Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            OutlinedButton(onClick = { onNavigate(8) }) { Text("Abrir scanner", fontSize = 12.sp) }
            OutlinedButton(onClick = { picker.launch(arrayOf("application/json", "text/plain", "*/*")) }) {
                Text("Importar JSON", fontSize = 12.sp)
            }
        }
        HubLabel("Importação experimental de PGSStats.json: extrai apenas Pokémon, PC e IVs quando os campos são reconhecidos. O arquivo original não é enviado à internet.")
        if (status.isNotBlank()) HubLabel(status)
        val scansToShow = minOf(10, scannerItems.length())
        for (i in 0 until scansToShow) {
            val item = scannerItems.optJSONObject(i) ?: continue
            HubLabel("✓ " + item.optString("name", "Pokémon") + " · PC " +
                item.optString("cp", "—") + " · IV " + item.optString("iv", "—"))
        }
        for (i in 0 until minOf(20, imported.length())) {
            val item = imported.optJSONObject(i) ?: continue
            HubLabel("• " + item.optString("name", "Pokémon") + " · PC " +
                item.optString("cp", "—") + " · IV " + item.optString("iv", "—"))
        }
        if (scannerItems.length() + imported.length() == 0) {
            HubLabel("Nenhum Pokémon salvo. Use o scanner ou importe uma exportação JSON.")
        }
    }
}

@Composable
private fun HubPvP(onNavigate: (Int) -> Unit) {
    HubBox("PvP — IV e ligas") {
        HubLabel("Grande Liga até PC 1500 · Ultra-Liga até PC 2500 · Liga Mestra sem limite de PC.")
        HubLabel("Um IV 100% não é necessariamente o melhor IV da Grande Liga. Rankings dependem de espécie, forma, nível, PC e Game Master GO.")
        OutlinedButton(onClick = { onNavigate(8) }) { Text("Analisar avaliação no scanner") }
        HubLabel("Rank IV por liga, ranking geral, equipes, golpes e previsão de turnos: integração com motor PvPPokeGo ainda pendente. Nenhum ranking fictício será exibido.")
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
        listOf("Coordenadas", "GO Rocket", "Inventário").forEach { option ->
            val selected = option == tab
            TextButton(onClick = { tab = option }, modifier = Modifier.weight(1f)) {
                Text(option, color = if (selected) hubAccent else hubText, fontSize = 10.sp)
            }
        }
    }
    when (tab) {
        "Coordenadas" -> HubCoordinates()
        "GO Rocket" -> HubRocket()
        else -> HubReferences()
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
