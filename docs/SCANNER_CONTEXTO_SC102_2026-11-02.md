# PokeGoBR 0.0.10 — Scanner por contexto (SC-102)

Base: auditoria estática do APK Poke Genie 8.19.1 fornecido pelo usuário em 02/11/2026. O APK não contém fonte editável confirmada; as rotinas abaixo são implementação independente, sem cópia de DEX, modelos, imagens, bancos ou classes proprietárias.

## Implementado

- Novo ScreenContextClassifier.kt, classe Kotlin pura e testável.
- Classificação conservadora em APPRAISAL, DETAILS, MOVES, RAID e OTHER.
- APPRAISAL exige três barras detectadas pelo nosso detector V2. O botão "Avaliar" sozinho NÃO é aceito como prova de uma avaliação aberta.
- DETAILS exige PC e PS na mesma captura, ou um campo com vocabulário de detalhes.
- MOVES exige rótulos específicos de golpes, nunca simplesmente o nome "Ataques" do menu.
- RAID exige contexto de batalha/chefe; a palavra "Raides" do menu não é suficiente.
- Se a tela é OTHER, a bolha informa que a captura funciona mas não atribui Pokémon, IV, nível ou rank fictícios.
- Na avaliação, o detector V2 pode exibir estimativas numéricas A/D/PS explicitamente marcadas como estimativas e sujeitas a conferência.
- Overlay continua flutuante, arrastável, com MediaProjection autorizada, escolha de app/tela completa e sem FLAG_SECURE.
- Testes de unidade Kotlin/JUnit com nove cenários, além dos testes visuais sintéticos em Python de builds anteriores.
- CI executa testes Android JVM e compilação Debug; publica APK e fonte no GitHub Releases.

## Deliberadamente não implementado nesta etapa

- OCR especializado de recortes nome/PC/PS, calibração em aparelho, detecção de nível/forma por ícone, identificação de golpes pelo Game Master.
- Ranking PvP GL/UL/ML e cálculo de CP por nível/IV. Não apresentar rankings sem motor Game Master versionado e testes.
- Pokédex completa offline (fonte/licenças/traduções/estatísticas por jogo). Arquitetura planejada; dados proprietários do Poke Genie não foram distribuídos.
- Raides online/lobbies automáticos.

## Critérios de QA

1. Captura de um app com autorização Android 14+: abrir Pokémon GO, tocar Avaliar, bolha deve mostrar "Avaliação detectada".
2. Detalhes antes de tocar Avaliar: bolha deve mostrar "Detalhes do Pokémon" se PC+PS aparecem no OCR, sem IV.
3. Tela comum sem dados ou menu PokeGoBR: "Captura funcionando", sem dados antigos.
4. Tela de raide: só sinalização preliminar, sem inventar chefe ou timer.
5. Repetir com alturas/larguras diferentes, idioma PT-BR e EN e overlays parcialmente encobertos; falhas devem ser explícitas.
6. Confirmar permissão de print e que a sessão pode ser encerrada pelo usuário.

## Continuidade

Próxima fase: SC-103 (calibração de regiões relativas) + SC-104 (OCR de campos) com testes em capturas reais; depois SC-105 motor PvP e SC-106 Pokédex própria, somente dados licenciados e Game Master atualizado.

Segurança de produto: usar imagens localmente para OCR. O APK do Poke Genie foi usado como referência de arquitetura, não incorporado como dependência.
