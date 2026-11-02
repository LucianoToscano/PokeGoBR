# ARQUIVO MESTRE POKEGOBR — Integração v0.0.13
Checkpoint: 02/11/2026 | Base anterior: v0.0.12 build #16
Repositório oficial: https://github.com/LucianoToscano/PokeGoBR

## Escopo e proveniência

O usuário forneceu APKs/XAPKs e screenshots de seis aplicativos externos: Poke Genie, Calcy IV, PokeRaid, Velocity, PokeList e PGTools Data Hub. Forneceu também o inventário estruturado de **54 arquivos físicos**, com prioridades **P0=11, P1=8, P2=12, P3=23**, e um ZIP Inventário Projetos contendo uma única cópia textual do inventário de 31/10/2026, com 95 fontes principais, 17 complementares e 72 links históricos sobrepostos.

Nenhum desses índices comprova que uma API esteja ativa, que sua licença cubra redistribuição, ou que seus arquivos de código estejam fisicamente disponíveis. O inventário original inclui links privados do Google Drive, referências de credenciais e exportações pessoais. Não publicar esses dados nem incorporar APKs/DEX/modelos/sprites/bancos proprietários. O PokeGoBR inclui somente uma lista pública sanitizada de 54 nomes, tipos, prioridades e áreas; nenhuma referência privada de Drive.

## Matriz de execução

| Área | Implementação em 0.0.13 | Limites |
|---|---|---|
| Scanner de IV | Captura consentida MediaProjection, OCR ML Kit, detector V2, automático/por toque | Alfa; validar no aparelho |
| Classificador de tela | Avaliação, detalhes, golpes e raide | Heurístico; depende de OCR e visibilidade |
| Raides | OCR de rótulos explícitos de chefe, tempo, jogadores | Sem sala online ou convites |
| Pokédex | Busca por nome/número, índice offline inicial e botão para consultar PokéAPI com cache local | Taxonomia geral, não Game Master Pokémon GO |
| Meus Pokémon | Histórico do scanner e importação JSON local de PGSStats-style | Experimental; importa apenas campos reconhecidos e preserva privacidade |
| PvP | Regras informativas GL/UL/ML, separação de IV geral e ranking | Sem ranking por espécie, lutas ou turnos |
| Calculadoras | IV 0–15 e percentual /45, estimativa de estrelas | Sem custos de fortalecimento e Stardust |
| Trocas | Lista local | Sem leilão/negociação online |
| Itens | Notas locais | Não lê mochila do jogo |
| Comunidade | Compartilhamento voluntário de texto via Intent | Sem chat ou grupo integrado |
| Coordenadas | Parser local de par, geo URI ou link Maps, copiar e abrir mapa | Sem GPS, joystick ou teleporte |
| GO Rocket | Líderes de referência; explicita ausência de roster atualizado | Sem counters/escalações atuais |
| Arquivo Mestre | Pesquisa em 54 referências sanitizadas | Não são APIs instaladas ou binários integrados |

## Referências específicas

1. Poke Genie 8.19.1: OCR local, scanner, PvP, Pokédex, coleção, overlay; não copiado.
2. Calcy IV 3.5a: modos automático e por toque, simulação e banco de espécies/golpes; apenas comportamento estudado.
3. PokeRaid 0.49: scanner IV, Pokédex, coleção, PvP e raides; OCR inicial próprio v0.0.12, sem sala online.
4. Velocity 8.8.2: links e encaminhamento de coordenadas; parser próprio e consentimento para abrir mapa.
5. PokeList 1.3.18: busca, filtros, Rocket e Pokédex; não usamos automação de Acessibilidade.
6. PGTools Data Hub 2.5.3: Pokémon, raides, batalhas Max, Rocket e pontos; serviços externos não copiados.

## Fonte de dados e privacidade

A PokéAPI oferece um endpoint público GET /api/v2/pokemon-species?limit=2000, acionado pelo usuário e guardado em cache no aparelho. É uma taxonomia dos jogos Pokémon e não prova dados/estatísticas GO. O app não publica tokens, IDs do Drive, dados de conta ou PGSStats bruto. O importador experimental de coleção retém exclusivamente nome, PC e IV quando os dados são identificados, com limites de entrada. Não foi integrado código proprietário de outras empresas.

## Backlog P0/P1

P0 — Contrato canônico de espécie, forma, traje, golpes e estatísticas GO versionadas, origem/licença e migração de dados.
P0 — Conectar fonte própria PvPPokeGo 0.5.41, testar cálculos GL/UL/ML e combater discrepâncias Game Master.
P1 — Conector PokeSearch/PoGoCoordinates com autenticação por usuário, quotas, Retry-After e flags Shiny.
P1 — Integrar calculadora Stardust/EduHunter do código editável após testes e auditoria das tabelas de custos.
P1 — Coleção SQLite/Room e importadores versionados de PGSStats e PGSData (Java serializado não é JSON).
P2 — Raides com sala própria autenticada/moderada, Rocket/Power Spots com dados datados, chat/WhatsApp mediante autorização.
P2 — Testes reais Android 14–16, OCR, performance, permissão, redimensionamento e backups.

## Build/merge/publicação

GitHub Actions extrai a fonte original ZIP e aplica scanner_v003.py até scanner_v013.py; valida testes Python, JVM e compila Gradle testDebugUnitTest + assembleDebug. Publica APK debug, fonte editável consolidada e SHA256SUMS em GitHub Releases. O catálogo sanitizado está dentro dos assets; não incluir os APKs externos, credenciais ou exportações pessoais em commits públicos.

Não declarar funcionalidades futuras como prontas. Compilação bem-sucedida não substitui teste físico.
