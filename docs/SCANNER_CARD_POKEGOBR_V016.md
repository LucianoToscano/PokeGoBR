# PokeGoBR 0.0.16 — Scanner visual próprio e comparativo com Poke Genie

Data: 2026-11-02. Base anterior: PokeGoBR v0.0.15 build #23.
Fontes de referência fornecidas pelo proprietário do projeto: 44 screenshots JPG dentro do ZIP Poke Genie.zip, APK Poke Genie 8.19.1, CSV do Poke Genie e histórico Calcy IV. Autorização de uso de recursos alegada pelo usuário; nenhum byte de código/modelo, arte proprietária, recursos de conta ou exportações pessoais foi incorporado nesta entrega.

## Diferenças observadas nas telas de referência

O Poke Genie mostra um pipeline completo de IV: sobreposição/início de iVision, bolha de resultados resumidos no próprio Pokémon GO, card de percentual e 3 IVs, notas de IV excelente, classificação PvP por evolução e liga, histórico com sprites e badges, ficha detalhada por espécie, tipos/estatísticas, lista de golpes, cálculo de PC/nível/poeira/candy, cadeia de evolução, simulações, tabelas de eficiência e lista de raids. As telas também mostram o compartilhamento de apenas um aplicativo no Android.

O PokeGoBR 0.0.15, por sua vez, mostra texto OCR de diagnóstico ("PESO" como nome no antigo 0.0.13), estimativas de IV e bolha pequena sem um card visual completo, apenas catálogos e ranks importados estáticos. Uma equivalência funcional de todo o aplicativo de referência NÃO está implementada.

## O que é realmente novo na 0.0.16

1. Um card flutuante próprio, separado da bolha de captura: fundo translúcido claro, borda fina, tipografia legível, indicador circular de IV percentual, espécie, PC, PS, atributos Ataque/Defesa/PS e estrelas.
2. Arraste pelo cabeçalho, feche sem parar o serviço, solicite "Novo scan" e salve uma **estimativa** na coleção privada com confirmed=false. Nunca marca estimativa como IV verificado.
3. Após uma avaliação com 3 barras válidas, pausa a leitura automática até o usuário iniciar outra, reduzindo o risco de as próprias informações do card interferirem no OCR.
4. Se o usuário importar Poke Genie ou Calcy IV, o scanner tenta encontrar ranks históricos com **a mesma espécie e exatamente os mesmos IVs**; exibe GL/UL com nome da fonte. Quando não há correspondência, não inventa ranking.
5. Os campos de espécie e CP/PS utilizam o classificador e parser conservadores da 0.0.14. Nome ausente continua desconhecido; não pode voltar a PESO.
6. O scanner MediaProjection consentido, o classificador de contexto, os modos automático/toque, a Pokédex, o histórico, as importações privadas, o banco de 54 referências e o OCR conservador de raides permanecem preservados.

## Testes

Testes JVM verificam soma/percentual/estrelas de 1/15/14 (67% e 2★), 3/15/12, 15/15/15, rejeição de IV incompleto ou maior que 15 e manutenção separada de ranks PvP importados. CI também compila APK debug, executa testes anteriores e verifica presença das integrações e ausência de FLAG_SECURE.

## Limites conhecidos e próximas etapas

- O detector das barras ainda exige validação instrumental em screenshots reais; não há acurácia estatística comprovada nem correspondência ao Poke Genie.
- A identificação de espécie está limitada ao catálogo em cache e à leitura OCR. Fantasias e formas não são identificadas automaticamente.
- Não há ainda reconstrução de level/custo GO, melhor evolução PvP, ranking PokeGoBR calculado, simulação de batalhas, banco de golpes/CP, raides online ou sincronização de coleção.
- A UI é original e compacta, **inspirada em fluxo e funcionalidades**, não uma cópia da identidade visual proprietária do Poke Genie.
- A análise de capturas mostrou que grandes painéis cobrindo a avaliação atrapalham a visão e potencialmente o OCR. O novo card surge apenas depois de uma leitura tentativamente válida; pode ser fechado/arrastado e reabrir com novo scan.

## Continuidade de build

Após scanner_v003.py até scanner_v015.py, aplicar scanner_v016.py. Esse patch copia LiveIvResultCard.kt, LiveScannerServiceV16.kt e LiveIvResultTest.kt, atualiza versionCode 16/versionName 0.0.16. GitHub Actions publica APK de teste, pacote editável reconstituível e checksums.
