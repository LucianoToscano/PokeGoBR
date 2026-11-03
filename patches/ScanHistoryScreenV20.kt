package br.com.pokegobr

import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val historyInk = Color(0xFF233A3A)
private val historyMuted = Color(0xFF667C7A)
private val historyAccent = Color(0xFF258F7F)
private val historyPanel = Color(0xF5F8FBF9)
private val historyOutline = Color(0x5F9CAFA7)

private fun inform(context: Context, message: String) {
    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
}

@Composable
private fun GlassArea(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier.background(historyPanel, RoundedCornerShape(17.dp))
            .border(.8.dp, historyOutline, RoundedCornerShape(17.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        content = content
    )
}

@Composable
private fun IvRing(score: Int?, size: Dp = 49.dp) {
    val ink = when {
        score == null -> Color(0xFF98A7A4)
        score == 100 -> Color(0xFFE4AB2C)
        score >= 80 -> Color(0xFF46A37F)
        score >= 60 -> Color(0xFF2D98B8)
        else -> Color(0xFF748891)
    }
    Box(Modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = 4.dp.toPx()
            val inset = stroke / 2f
            val rect = Size(this.size.width - stroke, this.size.height - stroke)
            drawArc(ink.copy(alpha = .14f), -90f, 360f, false,
                topLeft = Offset(inset, inset), size = rect, style = Stroke(stroke))
            if (score != null) drawArc(ink, -90f, 3.6f * score, false,
                topLeft = Offset(inset, inset), size = rect,
                style = Stroke(stroke, cap = StrokeCap.Round))
        }
        Text(score?.toString() ?: "—", color = ink, fontSize = 14.sp,
            fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun ScanRow(entry: SavedScan, onOpen: () -> Unit) {
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
        .background(Color(0xEAFBFCFA))
        .border(.6.dp, Color(0x4DB4C9C2), RoundedCornerShape(12.dp))
        .clickable(onClick = onOpen).padding(horizontal = 9.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(9.dp)
    ) {
        Box(Modifier.size(47.dp).clip(RoundedCornerShape(10.dp))
            .background(Color(0xFFE3F0E9)), contentAlignment = Alignment.Center) {
            Image(painterResource(R.drawable.menu_pokemon),
                contentDescription = null, modifier = Modifier.size(34.dp))
            if (entry.number != null) Text("#" + entry.number,
                modifier = Modifier.align(Alignment.BottomCenter)
                    .background(Color(0xDCE9F2ED), RoundedCornerShape(4.dp))
                    .padding(horizontal = 2.dp), color = historyInk, fontSize = 8.sp)
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(entry.name, fontSize = 14.sp, fontWeight = FontWeight.Bold,
                color = historyInk, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("PC " + (entry.cp ?: "—") + " · PS " + (entry.hp ?: "—") +
                " · IV " + (entry.iv?.joinToString("/") ?: "—"),
                fontSize = 10.8.sp, color = historyInk, maxLines = 1,
                overflow = TextOverflow.Ellipsis)
            val ranks = listOfNotNull(
                entry.gl?.let { "GL #$it" }, entry.ul?.let { "UL #$it" }
            ).joinToString(" · ")
            Text(entry.source +
                (if (entry.own) if (entry.verified) " · Conferido" else " · Estimativa"
                 else " · Importado") +
                (if (ranks.isNotBlank()) " · $ranks" else ""),
                fontSize = 9.sp, color = historyMuted, maxLines = 1,
                overflow = TextOverflow.Ellipsis)
        }
        IvRing(entry.percent)
    }
}

@Composable
internal fun ScanHistoryScreen(
    onNavigate: (Int) -> Unit, modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var reload by remember { mutableIntStateOf(0) }
    var records by remember { mutableStateOf<List<SavedScan>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var query by remember { mutableStateOf("") }
    var filter by remember { mutableStateOf(ScanFilter.ALL) }
    var ordering by remember { mutableStateOf(ScanSort.DATE) }
    var descending by remember { mutableStateOf(true) }
    var sortMenu by remember { mutableStateOf(false) }
    var selectedKey by remember { mutableStateOf<String?>(null) }
    var selectedBucket by remember { mutableStateOf<String?>(null) }
    var confirmDelete by remember { mutableStateOf<SavedScan?>(null) }
    var exportItems by remember { mutableStateOf<List<SavedScan>>(emptyList()) }

    LaunchedEffect(reload) {
        loading = true
        records = withContext(Dispatchers.IO) { ScanHistoryStore.load(context) }
        loading = false
    }
    val shown = remember(records, query, filter, ordering, descending) {
        ScanHistoryStore.select(records, query, filter, ordering, descending)
    }
    val selection = records.firstOrNull {
        it.key == selectedKey && it.bucket == selectedBucket
    }
    val csvPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val ok = withContext(Dispatchers.IO) {
                    runCatching {
                        context.contentResolver.openOutputStream(uri)?.bufferedWriter()
                            ?.use { it.write(ScanHistoryStore.exportCsv(exportItems)) }
                            ?: error("Arquivo não disponível")
                    }
                }
                inform(context, if (ok.isSuccess) "Histórico CSV exportado"
                       else "Falha ao exportar histórico")
            }
        }
    }
    val importPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) scope.launch {
            val outcome = withContext(Dispatchers.IO) {
                runCatching { HubImportV15.readAll(context, uri) }
            }
            if (outcome.isSuccess) {
                inform(context, "Importação concluída: " + outcome.getOrThrow().count)
                reload++
            } else inform(context, outcome.exceptionOrNull()?.message ?: "Falha na importação")
        }
    }
    val photoPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        val entry = selection
        if (uri != null && entry != null && entry.own) scope.launch {
            val outcome = withContext(Dispatchers.IO) {
                runCatching { ScanPhotoStore.attach(context, entry, uri) }
            }
            if (outcome.isSuccess) {
                inform(context, "Captura anexada ao scan")
                reload++
            } else inform(context, outcome.exceptionOrNull()?.message ?: "Falha na imagem")
        }
    }

    if (confirmDelete != null) AlertDialog(
        onDismissRequest = { confirmDelete = null },
        title = { Text("Excluir digitalização?") },
        text = { Text("Esse registro será removido do histórico local. " +
            "Uma captura anexada também será excluída. Não é possível desfazer.") },
        confirmButton = {
            TextButton(onClick = {
                val entry = confirmDelete
                confirmDelete = null
                if (entry != null) scope.launch {
                    val ok = withContext(Dispatchers.IO) {
                        ScanHistoryStore.delete(context, entry)
                    }
                    inform(context, if (ok) "Registro excluído"
                                   else "Registro mudou. Atualize a lista.")
                    selectedKey = null; selectedBucket = null; reload++
                }
            }) { Text("Excluir", color = Color(0xFFC04448)) }
        },
        dismissButton = { TextButton(onClick = { confirmDelete = null }) {
            Text("Cancelar")
        } }
    )

    Column(modifier.fillMaxSize().padding(horizontal = 10.dp, vertical = 5.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (selection != null) {
            ScanHistoryDetails(
                entry = selection,
                onBack = { selectedKey = null; selectedBucket = null },
                onPhoto = { photoPicker.launch(arrayOf("image/*")) },
                onEdit = { values, checked ->
                    scope.launch {
                        val ok = withContext(Dispatchers.IO) {
                            ScanHistoryStore.update(context, selection, values, checked)
                        }
                        inform(context, if (ok) "IVs atualizados" else "Falha ao atualizar")
                        reload++
                    }
                },
                onCopy = {
                    scope.launch {
                        val ok = withContext(Dispatchers.IO) {
                            ScanHistoryStore.copyImported(context, selection)
                        }
                        inform(context, if (ok) "Cópia criada em Scans"
                                       else "Não foi possível criar a cópia")
                        selectedKey = null; selectedBucket = null; reload++
                    }
                },
                onDelete = { confirmDelete = selection },
                modifier = Modifier.weight(1f)
            )
        } else {
            GlassArea(Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Meus Pokémon", color = historyInk, fontSize = 19.sp,
                            fontWeight = FontWeight.Bold)
                        Text(records.size.toString() + " registros · " +
                            records.count { it.own } + " scans próprios",
                            color = historyMuted, fontSize = 11.sp)
                    }
                    OutlinedButton(onClick = { onNavigate(8) }) {
                        Text("Scanner", fontSize = 11.sp)
                    }
                }
                OutlinedTextField(value = query, onValueChange = { query = it.take(75) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Pesquisar nome, número, PC ou IV") },
                    singleLine = true)
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    ScanFilter.entries.forEach { f ->
                        FilterChip(selected = filter == f,
                            onClick = { filter = f },
                            label = { Text(f.label, fontSize = 10.sp) })
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    Box {
                        OutlinedButton(onClick = { sortMenu = true }) {
                            Text("Ordenar: " + ordering.label, fontSize = 10.5.sp)
                        }
                        DropdownMenu(expanded = sortMenu, onDismissRequest = {
                            sortMenu = false
                        }) {
                            ScanSort.entries.forEach { option ->
                                DropdownMenuItem(
                                    text = { Text(option.label) },
                                    onClick = { ordering = option; sortMenu = false }
                                )
                            }
                        }
                    }
                    TextButton(onClick = { descending = !descending }) {
                        Text(if (descending) "↓" else "↑",
                            color = historyAccent, fontSize = 20.sp)
                    }
                    Spacer(Modifier.weight(1f))
                    Text(shown.size.toString() + " encontrados", color = historyMuted,
                        fontSize = 10.sp)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = {
                        importPicker.launch(arrayOf("text/csv","application/json",
                            "application/zip","*/*"))
                    }) { Text("Importar", fontSize = 11.sp) }
                    TextButton(onClick = {
                        exportItems = shown.toList()
                        csvPicker.launch("PokeGoBR-Scans.csv")
                    }, enabled = shown.isNotEmpty()) {
                        Text("Exportar CSV", fontSize = 11.sp)
                    }
                    TextButton(onClick = { reload++ }) {
                        Text("Atualizar", fontSize = 11.sp)
                    }
                }
            }
            if (loading) {
                LinearProgressIndicator(Modifier.fillMaxWidth())
            } else if (shown.isEmpty()) {
                GlassArea(Modifier.fillMaxWidth()) {
                    Text("Nenhuma digitalização encontrada", color = historyInk)
                    Text("Toque em Scanner para analisar um Pokémon e salvar o " +
                        "resultado, ou importe seus CSVs.", color = historyMuted,
                        fontSize = 12.sp)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    contentPadding = PaddingValues(bottom = 18.dp)
                ) {
                    items(shown, key = { it.bucket + ":" + it.key }) { scan ->
                        ScanRow(scan) {
                            selectedKey = scan.key
                            selectedBucket = scan.bucket
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ScanHistoryDetails(
    entry: SavedScan, onBack: () -> Unit, onPhoto: () -> Unit,
    onEdit: (List<Int>, Boolean) -> Unit, onCopy: () -> Unit,
    onDelete: () -> Unit, modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var attributes by remember(entry.key, entry.signature) {
        mutableStateOf(entry.iv?.toList() ?: listOf(0, 0, 0))
    }
    var verified by remember(entry.key, entry.signature) {
        mutableStateOf(entry.verified)
    }
    val score = attributes.sum()
    val percent = (score * 100.0 / 45.0).toInt()
    val dateString = entry.date?.let {
        SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("pt", "BR")).format(Date(it))
    } ?: "Data não registrada"
    val image = remember(entry.imagePath) {
        entry.imagePath?.let { path ->
            runCatching {
                BitmapFactory.decodeFile(path,
                    BitmapFactory.Options().apply { inSampleSize = 2 })
                    ?.asImageBitmap()
            }.getOrNull()
        }
    }
    Column(modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(9.dp)) {
        GlassArea(Modifier.fillMaxWidth()) {
            TextButton(onClick = onBack) { Text("‹ Voltar ao histórico") }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(66.dp).clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFFE5F2EA)), contentAlignment = Alignment.Center) {
                    Image(painterResource(R.drawable.menu_pokemon),
                        contentDescription = null, modifier = Modifier.size(48.dp))
                }
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(entry.name, fontSize = 21.sp, color = historyInk,
                        fontWeight = FontWeight.Bold)
                    Text(entry.source +
                        (entry.number?.let { " · #$it" } ?: ""),
                        fontSize = 11.sp, color = historyMuted)
                    Text("PC " + (entry.cp ?: "—") + " · PS " + (entry.hp ?: "—"),
                        color = historyInk, fontSize = 12.sp)
                }
                IvRing(entry.percent, 60.dp)
            }
            HorizontalDivider(color = historyOutline)
            Text("IV geral · " + (entry.percent?.toString() ?: "—") + "% · " +
                (entry.stars?.toString() ?: "—") + "★",
                color = historyAccent, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                listOf("ATAQUE","DEFESA","PS").forEachIndexed { index, label ->
                    Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(label, color = historyMuted, fontSize = 10.sp)
                        Text(attributes[index].toString() + "/15", color = historyInk,
                            fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        if (entry.own && entry.iv != null) Row(
                            verticalAlignment = Alignment.CenterVertically) {
                            TextButton(onClick = {
                                val copy = attributes.toMutableList()
                                copy[index] = (copy[index] - 1).coerceAtLeast(0)
                                if (copy != attributes) verified = false
                                attributes = copy
                            }) { Text("−", fontSize = 19.sp) }
                            TextButton(onClick = {
                                val copy = attributes.toMutableList()
                                copy[index] = (copy[index] + 1).coerceAtMost(15)
                                if (copy != attributes) verified = false
                                attributes = copy
                            }) { Text("+", fontSize = 19.sp) }
                        }
                    }
                }
            }
            Text(if (entry.verified) "✓ Conferido no jogo pelo usuário"
                else if (entry.own) "Estimativa visual; não confirmado"
                else "Valores da exportação (" + entry.source + ")",
                color = historyMuted, fontSize = 11.sp)
            if (entry.own && entry.iv != null) {
                Row(verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { verified = !verified }) {
                    Checkbox(checked = verified, onCheckedChange = { verified = it })
                    Text("Conferi os IVs na avaliação", color = historyInk, fontSize = 11.sp)
                }
                Button(onClick = { onEdit(attributes, verified) },
                    modifier = Modifier.fillMaxWidth()) { Text("Salvar correções") }
            } else if (!entry.own) {
                OutlinedButton(onClick = onCopy, modifier = Modifier.fillMaxWidth()) {
                    Text("Criar cópia editável em Scans")
                }
            }
            if (entry.hasPvp) {
                HorizontalDivider(color = historyOutline)
                Text("Rankings PvP da exportação", color = historyInk,
                    fontWeight = FontWeight.Bold, fontSize = 13.sp)
                entry.gl?.let { Text("Grande Liga · #$it", color = historyInk) }
                entry.ul?.let { Text("Ultra-Liga · #$it", color = historyInk) }
                entry.little?.let { Text("Liga Pequena · #$it", color = historyInk) }
                Text("Valores históricos da fonte. Não recalculados pelo PokeGoBR.",
                    color = historyMuted, fontSize = 10.sp)
            }
            Text("Digitalização: $dateString", color = historyMuted, fontSize = 11.sp)
        }

        GlassArea(Modifier.fillMaxWidth()) {
            Text("Imagem da avaliação", color = historyInk, fontWeight = FontWeight.Bold)
            if (image != null) {
                Image(bitmap = image, contentDescription = "Imagem privada anexada",
                    modifier = Modifier.fillMaxWidth().heightIn(max = 285.dp),
                    contentScale = ContentScale.Fit)
            } else {
                Text("Nenhuma imagem anexada a este registro.",
                    fontSize = 11.sp, color = historyMuted)
            }
            if (entry.own) {
                OutlinedButton(onClick = onPhoto) {
                    Text(if (entry.imagePath == null) "Anexar captura do aparelho"
                         else "Substituir captura")
                }
                Text("A imagem escolhida é salva apenas no armazenamento privado " +
                    "do aplicativo. O scanner não grava fotos automaticamente.",
                    fontSize = 10.sp, color = historyMuted)
            }
        }
        GlassArea(Modifier.fillMaxWidth()) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(onClick = {
                    val message = entry.name +
                        " · PC " + (entry.cp ?: "—") +
                        " · IV " + (entry.iv?.joinToString("/") ?: "—") +
                        " · " + (entry.percent ?: "—") + "%"
                    context.startActivity(Intent.createChooser(
                        Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, message)
                        }, "Compartilhar digitalização"))
                }) { Text("Compartilhar") }
                TextButton(onClick = onDelete) {
                    Text("Excluir", color = Color(0xFFC04448))
                }
            }
        }
    }
}
