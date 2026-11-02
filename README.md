# PokeGoBR
Central modular Android para Pokémon GO — versão **0.0.5 (Scanner ao vivo experimental)**.

## Funcional nesta versão
- IVs desconhecidos por padrão: resultados não podem ser salvos com IV fictício.
- Nova captura limpa os dados anteriores se o OCR não os identificar.
- Home visual compacta, paisagem completa e artes próprias Mewtwo, Charizard, logo e 11 ícones de navegação.
- Scanner por captura de tela (galeria do Android) com OCR ML Kit no aparelho.
- Reconhecimento inicial de PC/CP e PS/HP e revisão manual dos dados.
- Detector visual experimental das três barras coloridas; sugere valores apenas quando uma escala de comprimento estiver visível.
- IV calculado **somente** após os três valores serem preenchidos ou aceitos pelo usuário; sugestões não são confirmadas automaticamente.
- Raides: leitura inicial de texto de capturas, sem criação automática de lobby.
- Histórico local e sem transmissão de imagens.

## Código completo
A base está em `source/PokeGoBR-v0.0.2-SCANNER-FONTE.zip`; os patches auditáveis `patches/scanner_v003.py` e `patches/scanner_v004.py` atualizam para 0.0.4 durante a compilação. O detector é código próprio em `patches/AppraisalBarDetector.kt`. O build publica um ZIP da fonte consolidada, além do APK.

## Compilar
No GitHub: **Releases → PokeGoBR Scanner alfa** (APK publicado após CI verde) ou **Actions → Build PokeGoBR APK Debug → Artifacts**.
No computador (JDK 17 e Android SDK 35):
```bash
unzip -o source/PokeGoBR-v0.0.2-SCANNER-FONTE.zip -d .
bash ./gradlew :app:assembleDebug
```
O APK fica em `app/build/outputs/apk/debug/app-debug.apk`.

### Segurança
Sem root, injeção no Pokémon GO, cliente modificado ou captura silenciosa. Não reaproveita código de aplicativos comerciais.

### Limites da leitura visual (0.0.4)
O algoritmo detecta faixas alaranjadas alinhadas em três linhas. Ele só propõe A/D/PS se conseguir observar uma extensão de barra de referência compatível; caso contrário exige entrada manual. Ainda é preciso validar com capturas reais em diferentes idiomas, aparelhos e resoluções. Não oferece overlay em tempo real nem gerenciamento online de lobbies nesta versão.

### Scanner ao vivo 0.0.5
- MediaProjection solicita consentimento do Android para a sessão de captura da tela.
- Uma bolha flutuante arrastável exibe nome/PC/PS reconhecidos localmente com ML Kit e sugestões de barras de IV quando visualmente interpretáveis.
- Sem envio, gravação, upload de screenshots, root ou alteração do jogo.
- Requer permissão de sobreposição (`SYSTEM_ALERT_WINDOW`) e serviço de primeiro plano `mediaProjection`. O usuário pode encerrar pela bolha/notificação ou pelos controles do sistema.
- Requer testes em dispositivo real; app protegido por `FLAG_SECURE` ou alterações de tamanho/orientação podem limitar a leitura. Valores são estimativas e não são confirmados automaticamente.
