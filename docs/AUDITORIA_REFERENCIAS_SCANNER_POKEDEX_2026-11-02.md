# Auditoria técnica — Poke Genie, Calcy IV e PokeRaid
Data: 02/11/2026 · Referências: Google Drive / 05_Referencias_Externas
Escopo: inspeção estática autorizada dos pacotes fornecidos pelo usuário. **Não houve execução de código de terceiros, nem cópia de modelos, bancos, recursos gráficos ou DEX no PokeGoBR.** A presença de classes ou dados não comprova seu uso em determinada funcionalidade.

## Identificação e conteúdo

| Aplicativo | Formato/tamanho | Captura e leitura | Dados locais/Pokédex | Observações |
|---|---|---|---|---|
| Poke Genie | APK, 113.164.705 bytes | 10 DEX; termos MediaProjection, createScreenCaptureIntent, ImageReader e tesseract; 5 arquivos traineddata | Recursos CSV (exp, características, etc.), componentes com termos Pokedex | 16 libs nativas ARM; não foi reproduzido nenhum mecanismo proprietário |
| Calcy IV 3.5a | XAPK, 31.138.118 bytes | Base tesmath.calcy.apk e splits ARM64/xxhdpi; termos MediaProjection, ImageReader e tesseract; 5 arquivos OCR traineddata | Banco SQLite `assets/gamestats/gamestats.db` com tabelas BaseMonsters, Monsters, Moves, Evolutions, MonsterMoves, MonsterEliteTmMoves, MonsterLegacyMoves, Localizations, ShadowSettings | Referência de arquitetura para catálogo offline, formas, espécies e traduções PT-BR; banco não é redistribuível por padrão |
| PokeRaid 0.49 | XAPK, 135.409.806 bytes | Base me.pokeraid.apk e splits arm64, pt e xxhdpi; termos MediaProjection, ImageReader e ML Kit; modelos TFLite para OCR/barcode | Componentes com termos Pokedex e dados de interface; uso exato requer testes funcionais | Mais foco em raides e funções de comunidade, sem API de lobby fornecida |

## Diagnóstico PokeGoBR v0.0.5–0.0.7
- v0.0.5 e v0.0.6 usavam `createConfigForDefaultDisplay()`, suprimindo a opção de compartilhar somente um app. Corrigido em 0.0.7.
- `LiveScannerService` calculava dimensões somente uma vez, mesmo quando o Android 14+ alterava a área compartilhada. A revisão 0.0.8 acompanha `onCapturedContentResize()` e troca a `Surface` da mesma projeção (sem reutilizar o token de permissão).
- A leitura original reciclava o bitmap intermediário antes de terminar a análise sem garantir que ele era um objeto distinto. A revisão 0.0.8 evita o descarte prematuro; a documentação Android informa que o recorte pode ser o mesmo objeto.
- Erros de inicialização e de processamento antes eram capturados e descartados sem informação visível. A revisão 0.0.8 mostra um aviso curto na bolha ou toast e registra somente o tipo de falha no log técnico (sem texto da captura).
- Antes a bolha iniciava recolhida; agora mostra estado inicial e confirma o recebimento do primeiro quadro.

## Próximas tarefas após validar a captura
1. **Scanner**: medições do detector V2 em diversas resoluções/idiomas, orientação, oclusão e formas; métricas de falso positivo e abstenção. Não confiar em OCR de IVs ausentes.
2. **Pokédex offline própria**: catálogo normalizado por espécie, forma, trajes, evolução, golpes, traduções e Game Master versionado. Utilizar datasets com licença adequada (por exemplo PokeAPI e PoGoAPI conforme campos aplicáveis), não extrair o SQLite do Calcy para distribuição.
3. **PvP/PvE**: combinar scanner confirmado com motor de ranks do PvPPokeGo e calculadoras autorizadas; CP/HP devem ser validados pelo Game Master de Pokémon GO.
4. **Raides**: OCR do chefe e timer, sala opcional com infraestrutura e moderação próprias.

## Limitações
Inspeção estática não permite garantir que os aplicativos externos usam cada API em uma tela específica. O APK de 0.0.8 deve ser testado no dispositivo Android 14–16: consentimento, seleção de app, primeiro frame, OCR e modo de bolha. O CI só valida compilação e testes estáticos/sintéticos, não uma sessão real de MediaProjection.
