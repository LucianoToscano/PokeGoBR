#!/usr/bin/env python3
"""Scanner v0.0.3: não inventar IV e não reaproveitar OCR de capturas anteriores."""
from pathlib import Path

root = Path(__file__).resolve().parent.parent
main = root / 'app/src/main/java/br/com/pokegobr/MainActivity.kt'
script = main.read_text(encoding='utf-8')

def replace_once(source, old, new, label):
    n = source.count(old)
    if n != 1:
        raise RuntimeError(f'{label}: esperado 1 trecho, encontrado {n}')
    return source.replace(old, new, 1)

for var in ('atk', 'def', 'sta'):
    script = replace_once(script,
        f'var {var} by rememberSaveable {{ mutableIntStateOf(15) }}',
        f'var {var} by rememberSaveable {{ mutableIntStateOf(-1) }}',
        f'IV inicial {var}')
script = replace_once(script,
    '''                        pc = info.cp?.toString() ?: pc
                        ps = info.hp?.toString() ?: ps
                        if (!info.candidateName.isNullOrBlank() && pokemonName.isBlank()) pokemonName = info.candidateName
                        scanning = false''',
    '''                        // Nova imagem não reaproveita dados da anterior.
                        pc = info.cp?.toString().orEmpty()
                        ps = info.hp?.toString().orEmpty()
                        pokemonName = info.candidateName.orEmpty()
                        atk = -1; def = -1; sta = -1
                        scanning = false''', 'reset na captura')
script = replace_once(script,
    '''                val score = atk + def + sta
                val pct = (100.0 * score / 45.0)
                val stars = when { score == 45 -> "4★"; score >= 37 -> "3★"; score >= 30 -> "2★"; score >= 23 -> "1★"; else -> "0★" }
                Text("IV: ${"%.1f".format(java.util.Locale.US, pct)}% · $stars · $atk/$def/$sta", color = cyan, fontWeight = FontWeight.Bold, fontSize = 17.sp)''',
    '''                val ivConfirmed = atk in 0..15 && def in 0..15 && sta in 0..15
                if (ivConfirmed) {
                    val score = atk + def + sta
                    val pct = (100.0 * score / 45.0)
                    val stars = when { score == 45 -> "4★"; score >= 37 -> "3★"; score >= 30 -> "2★"; score >= 23 -> "1★"; else -> "0★" }
                    Text("IV: ${"%.1f".format(java.util.Locale.US, pct)}% · $stars · $atk/$def/$sta", color = cyan, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                } else {
                    Text("IV desconhecido · informe Ataque, Defesa e PS", color = ice, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                }
                TextButton(onClick = { atk = -1; def = -1; sta = -1 }, modifier = Modifier.align(Alignment.End)) {
                    Text("Limpar IVs", color = ice)
                }''', 'IV confirmado')
script = replace_once(script,
    '''                }, modifier = Modifier.fillMaxWidth()) { Text("Salvar avaliação no aparelho") }''',
    '''                }, enabled = ivConfirmed && pokemonName.isNotBlank() && pc.toIntOrNull() != null,
                    modifier = Modifier.fillMaxWidth()) { Text("Salvar avaliação no aparelho") }''', 'salvamento validado')
script = replace_once(script, '            value = value.toFloat(),',
    '            value = value.coerceAtLeast(0).toFloat(),', 'slider indefinido')
script = replace_once(script,
    '        Text(value.toString(), modifier = Modifier.width(25.dp), textAlign = TextAlign.End, fontWeight = FontWeight.Bold, color = cyan)',
    '        Text(if (value < 0) "—" else value.toString(), modifier = Modifier.width(25.dp), textAlign = TextAlign.End, fontWeight = FontWeight.Bold, color = cyan)',
    'texto indefinido')
script = replace_once(script, 'ALFA · 0.0.2', 'ALFA · 0.0.3', 'versão scanner')
script = replace_once(script, 'Sobre · v0.0.2', 'Sobre · v0.0.3', 'versão menu')
main.write_text(script, encoding='utf-8')

gradle = root / 'app/build.gradle.kts'
code = gradle.read_text(encoding='utf-8')
code = replace_once(code, 'versionCode = 2', 'versionCode = 3', 'versionCode')
code = replace_once(code, 'versionName = "0.0.2"', 'versionName = "0.0.3"', 'versionName')
gradle.write_text(code, encoding='utf-8')
print('Scanner 0.0.3: patch aplicado')
