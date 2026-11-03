# PokeGoBR

Central modular Android para Pokémon GO — versão **0.0.20 (Scanner de contexto, captura ao vivo e detector V2)**.

## Funcional na 0.0.20

- **Histórico de scans nativo (inspirado no fluxo Poke Genie):** Meus Pokémon abre lista compacta de scans próprios/importados, círculo IV, pesquisa, filtros por origem/estado/PvP/100%, ordenação, ficha individual, edição e confirmação de IV (scans próprios), exclusão com confirmação, compartilhamento e exportação CSV. Registros importados podem ser copiados para edição sem alterar o histórico original. Anexo voluntário de captura via seletor de imagens, armazenado somente no aparelho. A versão não salva screenshots automaticamente. Consulte `docs/HISTORICO_SCANNER_POKEGENIE_v020_2026-11-03.md`.


- **GoDex integrado como fonte de coleção externa:** a aba **Extras → GoDex** abre o site em português, ShinyDex, Dynamax/Gigantamax e coleções públicas com links validados. A Pokédex e Meus Pokémon têm atalhos diretos. Abre no navegador Android, preservando o login e sem acessar cookies nem compartilhar dados da conta ou imagens do scanner. Sem API pública de sincronização confirmada; consulte `docs/INTEGRACAO_GODEX_V019_2026-11-03.md`.


- **Correção de captura em Treecko:** para evitar interpretar a própria interface do PokeGoBR como jogo, o scanner esconde bolha e card antes de fotografar o quadro, lê PC somente no topo e PS na região de nome, inclui Treecko/Grovyle/Sceptile offline. Após o reconhecimento, o novo card começa recolhido e pode ser expandido por toque. Testes de regressão para PC 152/PS 43 e rejeição de valores falsos da sobreposição. Ver `docs/SCANNER_TREECKO_OVERLAY_V018.md`.


- **Scanner v017:** no modo automático espera duas leituras compatíveis dos três IVs para reduzir resultados inconsistentes; identifica nome preferencialmente pela região central da ficha do Pokémon. No card, é possível ajustar Ataque/Defesa/PS com +/− e marcar explicitamente que os IVs foram conferidos no jogo antes de salvar. A confirmação é humana, e as estimativas automáticas continuam exigindo validação real. Leia `docs/SCANNER_ESTABILIDADE_REVISAO_V017.md`.


- **Scanner com card visual próprio:** aparece após a leitura tentativa de três barras. Mostra nome reconhecido, PC, PS, IV%, estrelas, Ataque/Defesa/PS em colunas, ranks PvP importados quando há correspondência da mesma espécie/IV e ações de arrastar, fechar, novo scan e salvar estimativa. O scanner continua sem comprovação de acurácia equivalente aos aplicativos comerciais. Leia `docs/SCANNER_CARD_POKEGOBR_V016.md`.


- **Importação multi-fonte:** Poke Genie CSV, Calcy IV histórico CSV, PGSStats.json e PGSStats.zip. Registros locais por origem, pesquisa de espécie/PC/IV e visualização de ranks GL, UL e LL com indicação da fonte original. PGSData.dat (Java serializado) é identificado e recusado por segurança. Os arquivos reais do usuário NÃO são publicados nem integrados ao APK; a seleção ocorre no próprio Android. Leia docs/IMPORTADORES_EXPORTACOES_v015_2026-11-02.md.


- **Scanner compacto revisado com base em capturas reais:** a bolha fica recolhida por padrão, libera a avaliação do jogo, abre controles somente ao toque e pausa a análise automática enquanto o menu está aberto. A leitura de nomes confirma correspondência com a Pokédex local (não aceita PESO como nome), reconhece PC281/PC 281 e 58/58 PS e apresenta IV visual estimado com percentual/estrelas em uma linha curta. Valores estimados permanecem sujeitos à validação no jogo. Consulte docs/CORRECAO_SCANNER_REAIS_FROAKIE_v014_2026-11-02.md.


- **Hub unificado com páginas próprias:** Pokédex de espécies (índice inicial offline + atualização voluntária PokéAPI e cache), Meus Pokémon (avaliações salvas + importação local experimental de PGSStats JSON), calculadora IV, PvP orientativo, Trocas e Itens como listas locais, Comunidade com compartilhamento de texto, Extras com parser de coordenadas, GO Rocket informativo e catálogo sanitizado de 54 referências. **Não são equivalentes aos motores completos dos aplicativos externos.**

- Nas telas de raide com rótulos explícitos, o scanner pode sugerir **chefe, tempo restante e participantes**, sempre marcados como leitura OCR sujeita a confirmação. Quando esses rótulos não aparecem, não inventa os dados.

- O scanner flutuante oferece agora **Automático** (varredura periódica) e **Por toque** (analisar uma única imagem ao pressionar **Escanear agora**). A opção manual evita OCR contínuo enquanto não houver solicitação.

- Scanner direto da tela do Android, com consentimento por sessão via MediaProjection; bolha flutuante arrastável que pode ser fechada pelo usuário.
- **Android 14 ou superior:** o seletor de captura oferece a opção de compartilhar somente o Pokémon GO (um app) ou toda a tela, conforme o sistema permitir. A 0.0.5/0.0.6 restringia indevidamente ao display inteiro.
- **0.0.8:** atualiza a superfície da captura quando a região compartilhada muda de tamanho (incluindo seleção de um app), indica quando um quadro foi recebido e mostra falhas de leitura em vez de encerrar silenciosamente.
- Corrige o ciclo de vida de bitmaps para não reciclar uma imagem antes de a leitura OCR concluir.
- Captura da galeria permanece como alternativa.
- OCR ML Kit local para nome, CP/PC e HP/PS, com revisão dos dados.
- Novo classificador independente de telas (Avaliação, Detalhes, Golpes, Raide e Outra). Exibe uma mensagem apropriada na bolha e não confunde o botão Avaliar com barras reais.
- Na tela de avaliação, somente mostra valores de IV tentativos quando as três barras foram detectadas, sempre exigindo conferência.
- **Detector V2 original:** localiza o trilho inteiro das três barras de IV (preenchimento laranja/vermelho + parte cinza) para tratar IV baixo e 15/15/15.
- Em caso de barras encobertas, avaliação ausente ou leitura ambígua, não deve inventar IV. As sugestões continuam experimentais e requerem conferência humana.
- Ao abrir nova imagem, dados antigos da galeria são descartados para evitar reutilização indevida.
- Home visual própria com 11 ícones, histórico local de Pokémon confirmados e leitura inicial de texto em raides.
- Nenhuma imagem enviada ou armazenada pelo scanner ao vivo; não exige root, modificação ou injeção no jogo.

## Arquitetura de fonte e compilação

O repositório contém a fonte original em `source/PokeGoBR-v0.0.2-SCANNER-FONTE.zip` e os patches sequenciais, aplicados pelo CI:

```bash
unzip -oq source/PokeGoBR-v0.0.2-SCANNER-FONTE.zip -d .
python3 patches/scanner_v003.py
python3 patches/scanner_v004.py
python3 patches/scanner_v005.py
python3 patches/scanner_v006.py
python3 patches/scanner_v007.py
python3 patches/scanner_v008.py
python3 patches/scanner_v009.py
python3 patches/scanner_v010.py
python3 patches/scanner_v011.py
python3 patches/scanner_v012.py
python3 patches/scanner_v013.py
python3 patches/scanner_v014.py
python3 patches/scanner_v015.py
python3 patches/scanner_v016.py
python3 patches/scanner_v017.py
python3 patches/scanner_v018.py
python3 patches/scanner_v019.py
python3 patches/scanner_v020.py
bash ./gradlew --no-daemon :app:testDebugUnitTest :app:assembleDebug
```

Requer JDK 17, Android SDK 35 e ambiente com Gradle configurado. Arquivos editáveis da build aparecem na área **Releases**, juntos do APK e SHA256SUMS. Não modifique a fonte zipada sem ajustar a sequência de patches. Código do detector integrado: `app/src/main/java/br/com/pokegobr/AppraisalBarDetector.kt` após os patches. Fonte de referência: `patches/AppraisalBarDetectorV2.kt`.

## Testes

```bash
python3 -m pip install numpy pillow
python3 -m unittest discover -s tests -p 'test_*.py' -v
```

Os testes de regressão em Python geram imagens sintéticas (1/15/14, 15/15/15, 0/0/0, imagem vazia, oclusão). O build Android compila o Kotlin. Foi também realizada inspeção offline com seis imagens de referência do `Scanners.zip` fornecido pelo usuário; as capturas **não** entram no repositório público. Esses testes não equivalem a teste em dispositivo real ou medição estatística de acurácia.

## Privacidade e limites

O serviço MediaProjection usa autorização do Android, notificação de captura em primeiro plano e pode ser encerrado por toque. A captura de apenas um aplicativo exclui conteúdo de outros apps e da interface do sistema, aumentando a privacidade; essa escolha pertence à interface do Android. Em versões mais antigas do Android, a opção de um único app pode não existir. O modo ao vivo é **alfa**: deve ser validado no Android 10–16, especialmente em transição de tela, rotação, proteção de screenshot, consumo de bateria e precisão das barras. Não há lobbies de raides online nesta versão.

O código implementa comportamento próprio e não redistribui código, imagens, modelos ou APKs proprietários de Poke Genie, Calcy IV e PokeRaid.

## Diagnóstico no aparelho
Depois de permitir a captura, a bolha começa mostrando **Iniciando captura** e passa a **Captura funcionando** quando o primeiro quadro é recebido. Se ocorrer erro, mostra o tipo da falha. Toque na bolha para recolher/expandir; use **Parar scanner** ou a notificação Android para encerrar. Se a bolha não aparecer, verifique a permissão **Aparecer sobre outros apps**. Se a sessão terminar ao bloquear a tela, inicie outra com novo consentimento.

## Referências de Pokédex e OCR
Ver `docs/AUDITORIA_REFERENCIAS_SCANNER_POKEDEX_2026-11-02.md` (somente fatos observáveis de arquivos autorizados). O próximo módulo de Pokédex será independente, com dados Game Master versionados e fontes licenciadas; nenhuma biblioteca proprietária de terceiros é copiada para o APK.

## Capturas de tela permitidas (0.0.9)
- Corrigido: a janela flutuante do scanner 0.0.5–0.0.8 incluía `WindowManager.LayoutParams.FLAG_SECURE`, que bloqueava prints/gravações da janela no Android. Essa flag foi removida.
- O scanner continua solicitando consentimento de captura por `MediaProjection` e processa imagens localmente. A correção não altera as proteções de privacidade de aplicativos externos.
- Teste em Android real: tire um print da home PokeGoBR e da bolha flutuante, e verifique se a leitura MediaProjection continua funcionando.

## Classificador de contexto (SC-102)
O código próprio ScreenContextClassifier.kt usa sinais visuais do detector V2 e textos de OCR, sem copiar o Poke Genie. O motor completo de Pokédex e rankings PvP ainda não foi implementado; não exibe cálculos inexistentes. Testes Kotlin com casos de avaliação, tela normal, golpes, raides e incerteza. Ver docs/SCANNER_CONTEXTO_SC102_2026-11-02.md.

## Referência Calcy IV 3.5a
O XAPK e as 37 capturas do Calcy IV foram inspecionados como referência para os modos de uso e a futura Pokédex. Banco proprietário, código DEX, modelos OCR e imagens externas NÃO são incluídos no PokeGoBR. Ver `docs/AUDITORIA_CALCY_IV_2026-11-02.md`. O simulador de fortalecimento, rankings PvP e dados offline são etapas futuras.

## Referência técnica PokeRaid 0.49
O XAPK e 25 capturas do PokeRaid foram inspecionados. A etapa 0.0.20 acrescenta um leitor próprio de campos de raide com nove testes unitários. As imagens demonstram com mais detalhe o scanner de IV, a coleção e o simulador, mas não comprovam o fluxo de salas online. Não copiamos código, modelos, ícones ou dados internos do PokeRaid. Ver `docs/AUDITORIA_POKERAID_2026-11-02.md`.

## Arquivo Mestre unificado (0.0.20)
Leia `docs/ARQUIVO_MESTRE_POKEGOBR_v013_2026-11-02.md`. Este documento distingue funcionalidades implementadas, parciais e pendentes, reunindo as seis auditorias estáticas de APK e o inventário histórico de 54 arquivos. O app não embute APKs/XAPKs, bibliotecas de terceiros proprietárias nem dados privados do Drive, e não oferece motor PvP GO, leilões nem salas de raides online nesta versão.

### Privacidade de dados importados
A coleção recebe dados de um JSON escolhido explicitamente pelo usuário. O importador local experimental só extrai campos reconhecidos de nome, PC e IV; não transfere dados à nuvem e não armazena código do treinador, conta, coordenadas, itens ou o JSON original. A Pokédex pública baixa somente taxonomia via PokéAPI a pedido do usuário, com cache local conforme boas práticas de uso da fonte.
