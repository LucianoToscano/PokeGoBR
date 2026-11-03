# PokeGoBR 0.0.20 — Auditoria Poke Genie e reconstrução do histórico

Data: 03/11/2026. Base: PokeGoBR 0.0.19 build #29.
Fontes fornecidas: APK Poke Genie 8.19.1, 44 screenshots, exportação CSV Poke Genie (446 registros), histórico Calcy IV (439), PGSStats, imagens reais do Pokémon GO. O usuário afirma ter autorização de desenvolvimento; código e recursos proprietários não são redistribuídos aqui.

## Recursos confirmados nas capturas do Poke Genie

- Meu Pokémon (445/446): lista com miniatura, espécie, PC/PS, IV, nível, movimentos, indicadores PvP e círculo de percentual IV; pesquisa, filtros e classificação.
- Ordenar por: ordem inversa, data de digitalização, data de captura, IV, PvP IV de Grande Liga, Ultra-Liga e Copinha, PC, PS, nome, número da Pokédex, nível, favorito, PC e PS máximos.
- Detalhes: ficha de espécie e atributos, 3 IVs, percentual e classificação, golpes e PvP por evolução, acesso a editar e excluir.
- Outros menus: simulação de fortalecimento/evolução, editor de golpes, Pokédex e estatísticas, comparação PvP e candidatos.
- Não há comprovação nas capturas de que o Poke Genie guarde permanentemente todas as imagens originais, nem de caminho de armazenamento exato de screenshots.

## Estado real da integração PokeGoBR 0.0.20

| Recurso | Estado |
|---|---|
| Lista compacta de Meus Pokémon com IV circular | Implementado |
| Pesquisa | Por nome, número, PC e IV |
| Filtros | Todos, Scans, Importados, Conferidos, Estimados, 100%, PvP |
| Ordenação | Data, nome, IV, PC, PS, Rank GL/UL, ordem reversa |
| Detalhe individual | Espécie, origem, IV, PC/PS, histórico e ranks de fonte |
| Edição/confirmado | Somente scans próprios, preservando importações intactas |
| Cópia editável de registro importado | Implementado |
| Exclusão | Com confirmação e limpeza da imagem anexada |
| Exportação | CSV por seletor Android, sem dados pessoais extras |
| Importação | Mantém CSV Poke Genie, CSV Calcy e PGSStats JSON/ZIP |
| Imagem | Anexo manual por escolha do usuário, guardado privadamente |
| Sprites individuais | Ainda não integrados; ícone próprio de fallback |
| Golpes, nível, custos, PvP novo | Pendentes de motor/dados Pokémon GO versionados |
| Screenshot automática | Não grava; imagens de scanner continuam temporárias na RAM |

## Armazenamento e compatibilidade

- Scans próprios: SharedPreferences pokegobr_scans, chave items, com historyId gerado para registros anteriores e novos UUID para scans novos; nenhuma limpeza do histórico existente.
- Exportações: SharedPreferences pokegobr_imported_collection, por origem (pokegenie, calcy, pgsstats); registro importado não é confundido com um indivíduo único.
- Capturas anexadas voluntariamente: armazenamento privado filesDir/pokegobr_scan_previews; mapa privado pokegobr_scan_images. Imagem limitada a 10 MB e redimensionada. Excluir scan também remove sua imagem.
- CSV exportado somente quando solicitado. Dados delimitados e strings escapadas; proteção contra fórmulas de planilha. Nenhum arquivo de usuário foi publicado no GitHub.

## Testes e validação

Testes sintéticos cobrindo Treecko PC152 PS43 IV0/12/15 (60%), Dragonite 15/15/15 (100%), Haunter importado com rank GL, filtros, IV desconhecido e CSV seguro. Build aplica patches v003–v020 em sequência, executa testes JVM/Python, compila APK debug e publica fonte editável reconstituível.

QA no aparelho ainda necessário para densidade, responsividade, anexo da imagem, longas coleções e regressão do scanner ao vivo.

## Próximas etapas

Sprites licenciados por espécie e forma; Game Master Pokémon GO versionado com nível, golpes, evoluções, custos e rankings atuais; opção de captura automática de recorte da avaliação sob consentimento; favoritos; histórico Room/SQLite e sincronização voluntária do GoDex após API autorizada. Nada disso é anunciado como implementado nesta versão.

O objetivo 0.0.20 é reproduzir a experiência de histórico e organização do Poke Genie, sem afirmar que o scanner já tem precisão ou motores completos equivalentes.
