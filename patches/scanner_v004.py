#!/usr/bin/env python3
"""Scanner v0.0.4: detector visual conservador de IV.
Executar após patches/scanner_v003.py.
"""
from pathlib import Path
import shutil

root = Path(__file__).resolve().parent.parent
main = root / 'app/src/main/java/br/com/pokegobr/MainActivity.kt'
script = main.read_text(encoding='utf-8')

def once(src, old, new, label):
    count = src.count(old)
    if count != 1:
        raise RuntimeError(f'{label}: esperado 1 trecho; encontrados {count}')
    return src.replace(old, new, 1)

script = once(script,
    '''    var rawText by remember { mutableStateOf("") }
    var pokemonName''',
    '''    var rawText by remember { mutableStateOf("") }
    var imageSelected by remember { mutableStateOf(false) }
    var barPreview by remember { mutableStateOf(AppraisalBarPreview()) }
    var pokemonName''', 'estado visual')
script = once(script,
    '''            scanning = true
            errorText = ""
            try {''',
    '''            scanning = true
            imageSelected = true
            errorText = ""
            rawText = ""
            barPreview = AppraisalBarPreview()
            try {''', 'reinicio a cada imagem')
script = once(script,
    '''                        rawText = recognized.text
                        val info = ScreenshotParser.parse(recognized.text)''',
    '''                        rawText = recognized.text
                        barPreview = if (raid) AppraisalBarPreview() else AppraisalBarDetector.inspect(context, uri)
                        val info = ScreenshotParser.parse(recognized.text)''', 'análise de barras')
script = once(script,
    '''        if (!raid) {
            FrostedPanel(Modifier.fillMaxWidth()) {
                Text("Conferir identificação", fontWeight = FontWeight.Bold, color = ice)''',
    '''        if (!raid) {
            FrostedPanel(Modifier.fillMaxWidth()) {
                if (imageSelected) {
                    Text("Barras da avaliação (experimental)", fontWeight = FontWeight.Bold, color = cyan)
                    Spacer(Modifier.height(5.dp))
                    Text(barPreview.description, color = ice, fontSize = 12.sp)
                    val proposed = barPreview.proposedIvs
                    if (proposed != null && proposed.size == 3) {
                        Spacer(Modifier.height(7.dp))
                        Text("Sugestão não confirmada: " + proposed.joinToString("/"), color = ice, fontWeight = FontWeight.SemiBold)
                        OutlinedButton(onClick = {
                            atk = proposed[0]; def = proposed[1]; sta = proposed[2]
                        }, modifier = Modifier.fillMaxWidth()) { Text("Aplicar sugestão e conferir IVs") }
                    }
                    Spacer(Modifier.height(11.dp))
                }
                Text("Conferir identificação", fontWeight = FontWeight.Bold, color = ice)''', 'interface detector')
script = once(script,
    '''                Text("Atenção: o resultado de IV acima depende dos valores preenchidos. Esta versão não interpreta automaticamente as barras da avaliação.", color = ice.copy(alpha = .7f), fontSize = 10.sp)''',
    '''                Text("IVs só são calculados a partir dos três valores confirmados. Sugestões visuais são experimentais, não são medidas oficiais do jogo.", color = ice.copy(alpha = .7f), fontSize = 10.sp)''', 'aviso')
script = once(script, 'ALFA · 0.0.3', 'ALFA · 0.0.4', 'versão scanner')
script = once(script, 'Sobre · v0.0.3', 'Sobre · v0.0.4', 'versão menu')
main.write_text(script, encoding='utf-8')

gradle = root / 'app/build.gradle.kts'
config = gradle.read_text(encoding='utf-8')
config = once(config, 'versionCode = 3', 'versionCode = 4', 'versionCode')
config = once(config, 'versionName = "0.0.3"', 'versionName = "0.0.4"', 'versionName')
gradle.write_text(config, encoding='utf-8')

source = root / 'patches/AppraisalBarDetector.kt'
dest = root / 'app/src/main/java/br/com/pokegobr/AppraisalBarDetector.kt'
if not source.is_file():
    raise RuntimeError('Fonte do detector visual ausente')
shutil.copyfile(source, dest)
print('Scanner 0.0.4: barra visual experimental, requer revisão dos IVs')
