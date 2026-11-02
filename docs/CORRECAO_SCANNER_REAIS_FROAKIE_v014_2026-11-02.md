# PokeGoBR 0.0.14 — Correção do Scanner IV a partir de screenshots reais
Data: 2026-11-02 | Base: v0.0.13 build #18.

## Problemas observados

O usuário enviou duas capturas do mesmo Froakie fantasia com PC 281, PS 58 e apelido contendo 3/15/12. A bolha anterior cobria a avaliação e as três barras; a primeira captura mostrou PC ausente e identificação falsa "Nome OCR: PESO", apesar de o nome verdadeiro ser Froakie. A segunda mostrou PC e estimativa visual ~3/15/12, mas sem resultado compacto, e o nome falso persistia. O scanner era mais próximo de um painel diagnóstico que de um produto de scanner.

## Correções desta etapa

- Overlay inicia recolhido, pequeno e arrastável; toque na bolha abre/fecha controles e detalhes. Leitura automática pausada enquanto controles estão abertos para diminuir interferência no próprio OCR; retomada após fechar.
- Escanear agora recolhe o painel antes de solicitar um quadro.
- Novo parser independente reconhece PC/CP colados ou separados: PC281, PC 281, CP 281; PS 58 e 58/58 PS.
- O identificador de espécie usa somente nomes encontrados no catálogo local. Se OCR devolver PESO ou outras etiquetas de interface, mantém espécie desconhecida em vez de mostrar um nome falso. Pode deixar nomes vazios quando catálogo for insuficiente.
- Índice inicial inclui Froakie/Frogadier/Greninja; demais espécies podem depender de atualização voluntária da PokéAPI, que fornece taxonomia e NÃO Game Master Pokémon GO.
- Detector visual, quando identifica três barras, mostra IVs estimados, percentual aproximado e estrelas (3/15/12 -> aprox. 66% e 2 estrelas). IV não é tratado como confirmado.
- Testes para Froakie com símbolos circulados, falso nome PESO, PC sem espaço, PS fracionado, limite de valores e IV incompleto.
- MediaProjection, modos Auto/Por toque, OCR de raides e módulos do hub preservados.

## Questões pendentes

Não há ainda algoritmo de scanner com precisão equivalente ao Poke Genie, Calcy IV ou PokeRaid; o ranking PvP por espécie, forma, nível, golpes e Game Master GO continua pendente. Validar em dispositivo real e medir acurácia com capturas de diferentes resoluções, skins, idiomas e IVs antes de proclamar equivalência. Próximo passo: card de resultado dedicado com posicionamento configurável e conjunto de testes instrumentados com imagens reais.

## Build e proteção dos dados

GitHub Actions aplica patches v003-v014, testa Kotlin e Python e publica APK/ZIP editável. Capturas privadas do usuário não foram adicionadas ao repositório.
