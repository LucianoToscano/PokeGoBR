# PokeGoBR

Central modular Android para Pokémon GO — versão **0.0.9 (Scanner ao vivo, captura por app e detector V2)**.

## Funcional na 0.0.9

- Scanner direto da tela do Android, com consentimento por sessão via MediaProjection; bolha flutuante arrastável que pode ser fechada pelo usuário.
- **Android 14 ou superior:** o seletor de captura oferece a opção de compartilhar somente o Pokémon GO (um app) ou toda a tela, conforme o sistema permitir. A 0.0.5/0.0.6 restringia indevidamente ao display inteiro.
- **0.0.8:** atualiza a superfície da captura quando a região compartilhada muda de tamanho (incluindo seleção de um app), indica quando um quadro foi recebido e mostra falhas de leitura em vez de encerrar silenciosamente.
- Corrige o ciclo de vida de bitmaps para não reciclar uma imagem antes de a leitura OCR concluir.
- Captura da galeria permanece como alternativa.
- OCR ML Kit local para nome, CP/PC e HP/PS, com revisão dos dados.
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
bash ./gradlew --no-daemon :app:assembleDebug
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
