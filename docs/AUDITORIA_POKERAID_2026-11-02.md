# Auditoria PokeRaid 0.49 — referência para PokeGoBR (02/11/2026)

## Materiais recebidos
- `PokeRaid_0.49 ... .xapk` (135.409.806 bytes), composto por quatro APKs: `me.pokeraid.apk`, `config.arm64_v8a.apk`, `config.pt.apk` e `config.xxhdpi.apk`. Pacote `me.pokeraid`, versão 0.49, `versionCode` 166, `minSdkVersion` 32 e `targetSdkVersion` 36 no manifesto XAPK.
- `PokeRaid.zip`: **25 screenshots** (18 screenshots nomeados Pokémon GO, 6 nomeados PokeRaid e 1 nomeado One UI Home). O arquivo ZIP não inclui código-fonte.
- A inspeção técnica foi **estática**. A permissão do usuário para estudar o material não significa que qualquer algoritmo proprietário tenha sido licenciado para redistribuição. Não copiamos DEX, SQLite, modelos ML Kit, imagens nem outros ativos do PokeRaid.

## Evidências observadas
- APK base com **8 arquivos DEX**; cerca de **71** entradas do APK base contêm `mlkit` no caminho e **39** contêm `tflite`. É uma contagem por nome, não uma prova do algoritmo empregado em cada tela.
- O XAPK declara `SYSTEM_ALERT_WINDOW`, `FOREGROUND_SERVICE_MEDIA_PROJECTION`, `FOREGROUND_SERVICE` e `POST_NOTIFICATIONS`, entre outras.
- O split ARM64 contém `libmlkit_google_ocr_pipeline.so`.
- Recursos incluem ícones de telas Pokédex, tipos, movimentos e PvP. Não foi identificado um banco Game Master completo pela listagem simples de arquivos.

## Fluxos registrados nas 25 imagens
- Tela Scanner de IV do PokeRaid com botão **Iniciar escaneamento**, controle de idioma do Pokémon GO e nível do treinador.
- Captura do diálogo Android para **Compartilhar um app**.
- Haunter de PC 1499 com A/D/PS **1/15/14**, percentual geral **67%** e **100% Grande Liga** exibido no fluxo; o resultado PvP é outro indicador, não o mesmo percentual de IV.
- Dragonite de IV 15/15/15, detalhes de golpes, fortalecimento e simulação de nível/custo.
- Coleção "Seus Pokémon" com filtros e resumo de IV, espécies, PC e golpes.
- Telas de listas PvP por liga/evolução e calculadora de estatísticas.
- **Limite importante:** não há sequência suficiente no ZIP para comprovar a implementação de salas online/convites ou a extração de chefe e timer em raides; os comportamentos não devem ser presumidos.

## Integração própria no PokeGoBR 0.0.12
- Novo `RaidScreenParser.kt` classifica campos explícitos na captura autorizada: título de raide, **Chefe de Reide/Raid Boss**, **Tempo Restante/Time Left**, **Treinadores/Trainers**.
- Os campos desconhecidos permanecem desconhecidos. Não converte horários arbitrários, nomes de perfil ou números de PC em dados de raide.
- A bolha do scanner exibe apenas esses dados de OCR, com indicação de que devem ser conferidos.
- Nove testes JUnit locais exercitam PT-BR, EN, OCR incompleto, contagens inválidas, menu do PokeRaid e tempos não identificados.
- Preserva MediaProjection autorizado, opção de compartilhar um app, modos Automático/Por toque, detector V2 e status da captura.

## Pendências
- Reconhecimento confiável de chefe/timer em screenshots reais de **raides** de Pokémon GO (não incluídos no ZIP).
- Identificação de espécie, nível, evolução e golpes a partir de catálogo próprio e Game Master versionado.
- Rankings PvP e simulação de fortalecimento com fórmulas validadas; ainda não disponíveis como funcionalidades prontas.
- Salas de raides reais precisariam de backend, autenticação, moderação e proteção contra abusos. Nenhum serviço/conta do PokeRaid foi acessado ou reutilizado.
- Testes em dispositivo físico Android 14–16 para compatibilidade de captura e precisão.

**Fontes:** XAPK e 25 capturas de tela enviados pelo usuário. Arquivos de terceiros não foram adicionados ao repositório.
