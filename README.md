# PokeGoBR
Central modular Android para Pokémon GO — primeira versão **0.0.2 (Scanner alfa)**.

## Funcional nesta versão
- Home visual compacta, paisagem completa e artes próprias Mewtwo, Charizard, logo e 11 ícones de navegação.
- Scanner por captura de tela (galeria do Android) com OCR ML Kit no aparelho.
- Reconhecimento inicial de PC/CP e PS/HP e revisão manual dos dados.
- IV calculado **somente** com os três valores fornecidos pela avaliação; não inventa precisão.
- Raides: leitura inicial de texto de capturas, sem criação automática de lobby.
- Histórico local e sem transmissão de imagens.

## Código completo
O pacote editável está em `source/PokeGoBR-v0.0.2-SCANNER-FONTE.zip` e inclui Kotlin, Gradle, assets e workflow. O build automático extrai o pacote e compila o APK debug.

## Compilar
No GitHub: **Actions → Build PokeGoBR APK Debug → Artifacts**.
No computador (JDK 17 e Android SDK 35):
```bash
unzip -o source/PokeGoBR-v0.0.2-SCANNER-FONTE.zip -d .
bash ./gradlew :app:assembleDebug
```
O APK fica em `app/build/outputs/apk/debug/app-debug.apk`.

### Segurança
Sem root, injeção no Pokémon GO, cliente modificado ou captura silenciosa. Não reaproveita código de aplicativos comerciais.
