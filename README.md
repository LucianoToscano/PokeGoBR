# PokeGoBR
Central modular Android para Pokémon GO — versão **0.0.3 (Scanner alfa corrigido)**.

## Funcional nesta versão
- IVs desconhecidos por padrão: resultados não podem ser salvos com IV fictício.
- Nova captura limpa os dados anteriores se o OCR não os identificar.
- Home visual compacta, paisagem completa e artes próprias Mewtwo, Charizard, logo e 11 ícones de navegação.
- Scanner por captura de tela (galeria do Android) com OCR ML Kit no aparelho.
- Reconhecimento inicial de PC/CP e PS/HP e revisão manual dos dados.
- IV calculado **somente** com os três valores fornecidos pela avaliação; não inventa precisão.
- Raides: leitura inicial de texto de capturas, sem criação automática de lobby.
- Histórico local e sem transmissão de imagens.

## Código completo
A base está em `source/PokeGoBR-v0.0.2-SCANNER-FONTE.zip`; o patch auditável `patches/scanner_v003.py` atualiza para 0.0.3 durante a compilação. O build publica um ZIP da fonte consolidada, além do APK.

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
