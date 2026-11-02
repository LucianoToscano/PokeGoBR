# Scanner PokeGoBR — plano de implementação

## Implementado (0.0.3 alfa)
- Importar screenshot local pela galeria; OCR ML Kit no próprio aparelho.
- Extrair sugestões de nome, PC e PS, sempre editáveis.
- IV A/D/S confirmado manualmente, com resultados desconhecidos até preenchimento.
- Histórico local com máximo de dez avaliações; modo Raides exibe texto OCR, sem hospedar lobbies.
- Nenhuma captura silenciosa, automação do cliente do jogo ou envio de imagem ao servidor.

## Próximas etapas, não implementadas
1. **Leitura de avaliação:** identificar visualmente três barras, estimar valores 0–15, medir confiança e exigir revisão quando ambíguo. Validar com conjunto real de screenshots (PT-BR, EN, múltiplas resoluções).
2. **Calculadora de IV:** usar base stats e tabela CPM versionada para deduzir nível e combinações de IV a partir de PC/PS, sem afirmar resultado único se forem possíveis múltiplas combinações.
3. **PvP:** ranks Grande/Ultra/Mestra por espécie, forma, nível, caps e IV; mostrar versão e proveniência dos dados.
4. **Scanner sobre tela:** MediaProjection com consentimento por sessão e foreground service; overlay opcional, sem root e sem interação automatizada com o Pokémon GO.
5. **Raides:** OCR de chefe, espécie, PC, tempo e localização de screenshots; compartilhamento voluntário. Hospedar/entrar em lobbies exige backend, privacidade, moderação e mecanismos antifraude.
6. **QA:** testes unitários de parser, barras e cálculos; benchmark em Android 10–16; acessibilidade e layouts compactos.

## Princípios
- Não inventar IVs, shiny, nível ou probabilidade quando a imagem não sustentar.
- Nunca coletar capturas sem ação/consentimento explícito; processamento local por padrão.
- Reconhecimento de texto sozinho não lê a altura das barras da avaliação.
- O scanner é criação própria e não incorpora código ou ativos proprietários de Poke Genie, Calcy IV ou PokeRaid.
