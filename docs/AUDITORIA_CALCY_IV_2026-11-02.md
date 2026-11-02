# Auditoria do Calcy IV 3.5a — referências para PokeGoBR (02/11/2026)

## Arquivos estudados
- Calcy IV_3.5a ... .xapk, 31.138.118 bytes; formato XAPK com base `tesmath.calcy.apk`, split ARM64 e split xxhdpi.
- Calcy IV.zip: 37 capturas de tela mostrando o fluxo do scanner sobre Pokémon GO, simulações, rankings, golpes e histórico.
- Identificação do pacote no manifesto XAPK: `tesmath.calcy`, versão 3.5a, minSdk 28, targetSdk 36.
- Permissões declaradas no XAPK: SYSTEM_ALERT_WINDOW, FOREGROUND_SERVICE, FOREGROUND_SERVICE_MEDIA_PROJECTION, INTERNET e POST_NOTIFICATIONS, entre outras. Permissões não provam o funcionamento de um recurso isoladamente.
- Arquivos de OCR no APK base: cinco `assets/tessdata/*.traineddata`, incluindo variantes para inglês, japonês, coreano e russo.
- `assets/gamestats/gamestats.db` — base SQLite local de aproximadamente 930 KB. A inspeção da estrutura revelou 16 tabelas, incluindo BaseMonsters (1024 entradas), Monsters (1222), Moves (365), Evolutions (521), MonsterMoves (7362), traduções (Localizations) e movimentos Elite/legado. Dados podem estar desatualizados e não estão sendo redistribuídos.

## O que as 37 capturas demonstram
1. Página inicial oferece **Automatic Scanning** ou **Click-To-Scan**.
2. Resultado flutuante compacto do Pokémon com IV, nível, estatísticas e indicador PvP.
3. Simulador de fortalecimento (nível alvo, PC, atributos e custos).
4. Rankings PvP por evolução e liga, exibidos em listas.
5. Comparação de golpes ágeis/carregados para PvP e raides.
6. Histórico próprio de Pokémon identificados e filtros.
7. As imagens de Dragonite 15/15/15 e Haunter 1/15/14 mostram a diferença entre IV geral e colocação PvP.

## Comparação com PokeGoBR
- PokeGoBR 0.0.10 já tem captura MediaProjection opt-in, OCR ML Kit, detector V2 de três barras e classificador de contexto.
- **Nova SC-103 / 0.0.11:** modos Automático e Por toque, com solicitação explícita Escanear agora na bolha. Código próprio FrameScanGate, com sete testes JVM (intervalo, toque único, alternância, tempo reiniciado).
- Os motores de custos de fortalecimento, Game Master/versionamento, Pokémon–golpes, rankings PvP/IV e Pokédex detalhada ainda não estão implementados; não exibir cálculos que não foram validados.

## Engenharia e privacidade
- Scanner lê a tela localmente somente após consentimento do Android; não usa injeção no processo do Pokémon GO.
- **Nenhum modelo, sprite, banco SQLite, código compilado ou recurso protegido do Calcy IV foi integrado.**
- O APK analisado é referência técnica; a inspeção estática não revela, por si só, a lógica real de todos os seus algoritmos.
- Testes físicos Android 14–16 ainda necessários para scanner por toque, estado da bolha e qualidade dos resultados.
