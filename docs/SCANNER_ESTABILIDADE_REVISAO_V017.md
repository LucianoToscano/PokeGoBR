# PokeGoBR 0.0.17 — Consenso visual e revisão manual do scanner

**Data:** 03/11/2026. **Base:** v0.0.16 build #24, merge PR #17. **Objetivo:** reduzir falsas leituras e dar ao usuário um fluxo de IV utilizável, com correção manual, sem apresentar valores estimados como confirmados.

## Problemas anteriores

- Um único quadro de avaliação que o detector V2 classificasse como três barras podia abrir o card imediatamente, mesmo com ruído ou uma captura parcialmente encoberta.
- A espécie podia ser confundida com nomes presentes no rodapé, como a indicação de doces ou energia da evolução.
- O card da v0.0.16 não permitia corrigir Ataque/Defesa/PS manualmente antes de salvar a captura.
- O histórico da v0.0.16 salvava qualquer resultado como estimativa não confirmada; não havia distinção quando o treinador verificava o dado.

## Implementação nova

1. **Consenso automático de dois quadros:** um objeto IvScanConsensus preserva temporariamente a assinatura (espécie, PC, 3 IVs) e só aceita uma leitura automática após a segunda leitura compatível, em até 8 segundos. Valores de IV devem ser exatamente iguais; espécies e PC conhecidos não podem conflitar. Sem dados válidos, reinicia. O status pode mostrar "Conferindo IV 1/2" durante a espera. O modo "Por toque" continua permitindo uma única análise, sempre rotulada estimativa.
2. **Identificação espacial por nome:** usa coordenadas das linhas OCR do ML Kit, priorizando a placa de nome da tela do Pokémon, em vez de procurar espécie em qualquer texto da captura. Se houver linhas com coordenadas mas nenhuma espécie na região, não assume o nome dos doces/evolução. O índice de espécies local continua necessário; formas e fantasias precisam de trabalho adicional.
3. **Ajuste manual no card:** três campos A/D/PS com controles + e −, limitados a 0–15. Cada mudança atualiza o percentual/estrelas e limpa rankings PvP importados previamente calculados para o IV anterior.
4. **Confirmação explícita e auditável:** controle "Conferi os IVs na avaliação", desmarcado por padrão. Quando marcado pelo usuário, salva no histórico privado com userConfirmed=true, origin=scanner_user_reviewed. Caso contrário, salva origin=scanner_estimate, confirmed=false. Ajustar qualquer IV desmarca a confirmação até nova revisão.
5. **Preservação das demais funções:** captura MediaProjection por sessão, modos auto/manual, readout PC/PS, cards compactos, histórico local, importações CSV/ZIP, PvP de fonte histórica e OCR inicial de raides.

## Limitações e QA

- "Duas leituras iguais" **não prova** que o IV está correto; prova apenas estabilidade das duas estimativas visuais. A confirmação voluntária é declaração do usuário, não verificação automatizada.
- O detector de barras V2 ainda necessita calibração usando uma grande amostra de screenshots de diferentes resoluções e estados da avaliação.
- A região OCR para nome é proporcional ao tamanho da captura; pode falhar em layout paisagem, tela dividida, proporções atípicas ou diálogos flutuantes.
- Espécies ausentes do índice local permanecem sem nome; não importar dados do APK do Poke Genie para preencher lacunas.
- Não há motor de PvP Game Master próprio recalculando ranks atuais, custos de fortalecimento nem simulação de movimentos.
- Teste obrigatório no dispositivo Samsung do usuário: Froakie de fantasia PC 281, PS 58, IV aparente 3/15/12; depois outro Pokémon e um hundo. Verificar status 1/2 antes do card no automático, comparar toques +/- e persistência na coleção, testar screenshots e rotação.

## Testes e build

O build aplica patches históricos v003 até v016 e por fim scanner_v017.py, copia IvScanConsensus.kt, LiveScannerServiceV17.kt, LiveIvResultCardV17.kt e IvScanConsensusTest.kt, muda versionCode para 17 e versionName 0.0.17. Executa unit tests e :app:assembleDebug no GitHub Actions e publica APK/ZIP completo de fonte com SHA256SUMS. Nenhuma captura ou exportação privada é inserida no repositório.
