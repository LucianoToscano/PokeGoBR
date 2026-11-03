# PokeGoBR 0.0.18 — Treecko, correção de captura e OCR espacial

Referência: capturas reais enviadas em 03/11/2026. Base v0.0.17 (build 25).
As imagens reais **não** foram enviadas ao repositório; apenas testes sintéticos e descrição.

## Defeito observado

O mesmo Treecko mostra **PC 152**, **PS 43** e avaliação visual com **A/D/PS aparentemente 0/12/15**. O painel PokeGoBR exibiu uma leitura esperada perto de 0/12/15, mas também mostrou **PC ausente**. Em outro quadro da mesma tela, o próprio painel exibiu incorretamente **PC 942 / PS 143 / 15/11/11**, aproximadamente 82%. O cabeçalho do card dizia "Pokémon não identificado", porque o índice offline inicial não incluía Treecko e a região de nome também podia ficar encoberta pela interface flutuante.

## Causas no código v017

1. MediaProjection captura sobreposições do próprio PokeGoBR ao compartilhar toda a tela; as barras podiam ser encobertas, e o algoritmo podia confundir gráficos do card com a avaliação.
2. OCR dos valores aceitava o fallback de tela inteira (ScreenshotParser) e não exigia que PC viesse da parte superior da tela.
3. O identificador de espécie dependia da lista inicial limitada a outras espécies ou atualização de PokéAPI.
4. O card aparecia totalmente aberto, tapando barras, o que prejudicava conferência humana.

## Correções v018

- **Frame limpo:** antes de processar o próximo quadro habilitado pelo FrameScanGate, oculta temporariamente a bolha e o card; não analisa o primeiro quadro, esperando pelo menos 350 ms para redesenho sem sobreposição. Restaura a bolha após copiar o bitmap. Um watchdog restaura controles quando o compartilhamento para de fornecer quadros.
- **Cabeçalho espacial:** PokemonHeaderParser usa coordenadas relativas das linhas OCR ML Kit. Aceita PC somente na faixa superior do Pokémon; procura nome na placa central e PS logo abaixo do nome; ignora valores em menus, cards flutuantes e linhas de poeira/doces. Não aceita PC/PS sem posição confiável.
- **Espécie offline:** adiciona Treecko, Grovyle e Sceptile ao catálogo inicial.
- **Card inicialmente compacto:** exibe espécie/PC/PS/IV% e A/D/PS resumidos. Toque no cabeçalho expande o painel com ajustes, confirmação humana, PvP importado e ações. Arraste pelo cabeçalho continua disponível. Posiciona inicialmente na região superior em vez de cobrir as barras.
- **Preservação:** consenso de duas leituras automáticas, modo por toque, correção de IV +/- e confirmação humana, histórico, importadores CSV/ZIP, OCR conservador de raides.

## Validação

Testes Kotlin sintéticos verificam PC 152/PS 43 na posição esperada, rejeição de PC 942/PS 143 de região de overlay, fragmentação OCR de PC e espécie presente só no rodapé. Compilação e testes JVM não substituem validação em dispositivo real, principalmente em tela dividida, resoluções distintas e overlay de terceiros.

Os IVs da imagem do usuário ainda precisam ser validados no Pokémon GO. O percentual é derivado matematicamente dos três IVs estimados; 0/12/15 = 27/45 = 60%.

## Arquivos e continuidade

Arquivos novos: PokemonHeaderParser.kt / PokemonHeaderParserTest.kt, LiveScannerServiceV18.kt, LiveIvResultCardV18.kt, UnifiedHubV18.kt, scanner_v018.py. Adicionar patch depois de scanner_v017.py na CI. Não sobrescrever arquivos históricos v017 usados em etapas anteriores. A fonte editável e a documentação devem integrar o ZIP do release.

## Pendências para equivalência ao Poke Genie

Medição quantitativa da acurácia do detector de barras em screenshots reais, OCR de formas/trajes, banco Pokémon GO versionado, CP/nível/custo e cálculos PvP oficiais. Não declarar esses motores prontos na v018.
