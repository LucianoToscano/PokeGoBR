# Scanner PokeGoBR — auditoria estática de referências (02/11/2026)

## Escopo e método
Arquivos disponibilizados pelo usuário na pasta Google Drive 05_Referencias_Externas. Inspeção estática local do contêiner ZIP/APK/XAPK: metadados, divisão de APKs, número de arquivos, DEX, bibliotecas nativas e evidências de modelos. **Nenhum aplicativo foi executado** e não foram copiadas classes, imagens, modelos proprietários, credenciais nem fluxos de rede.

| Referência | Nome do pacote | Versão | Formato | Evidências técnicas |
|---|---|---|---|---|
| Poke Genie Remote Raid IV PvP | não confirmada sem leitura do manifesto binário | não confirmada | APK, 113.164.705 bytes | 1.846 entradas; 10 arquivos DEX; 16 bibliotecas .so, ARMv7 e ARM64; 84 arquivos em assets |
| Calcy IV | tesmath.calcy | 3.5a | XAPK, 31.138.118 bytes | Package real: tesmath.calcy; 3 splits (base + ARM64 + xxhdpi), base com 1.184 entradas e 3 DEX; target SDK 36 |
| PokeRaid | me.pokeraid | 0.49 | XAPK, 135.409.806 bytes | 4 splits (base, ARM64, pt, xxhdpi); base com 2.052 entradas e 8 DEX; target SDK 36; presença de assets ML Kit OCR |

## Permissões observadas no manifesto XAPK
Calcy IV e PokeRaid declaram SYSTEM_ALERT_WINDOW, FOREGROUND_SERVICE e FOREGROUND_SERVICE_MEDIA_PROJECTION; ambos também declaram INTERNET, POST_NOTIFICATIONS e WAKE_LOCK. Essa evidência sugere tecnologias de overlay e captura autorizada, **não comprova** como essas funções são implementadas nem garante funcionamento em qualquer dispositivo. A leitura do Poke Genie não incluiu extração completa do manifesto binário.

## Arquitetura original adotada
1. OCR local ML Kit já incluído no PokeGoBR.
2. Detecção própria de barras vermelhas/alaranjadas da avaliação, sem invadir o processo do jogo; três linhas alinhadas e inferência numérica apenas diante de um comprimento total de referência visível.
3. UI exige conferência dos três valores antes do salvamento, evitando IVs fabricados.
4. MediaProjection com consentimento por sessão e overlay opcional são etapas futuras separadas, não parte da 0.0.4.
5. Lobbies de raide precisam de backend próprio, autenticação, abuso/moderação e privacidade; não há API terceirizada disponível demonstrada.

## Critérios de QA para próxima etapa
- Arquivar screenshots de testes autorizados em PT-BR e EN, com avaliação de 0, 1, 2, 3 e 4 estrelas, diferentes resoluções e orientação.
- Calcular precisão e falso positivo por A/D/PS, registrando os casos de escala incerta.
- Testes de correção de rotação/corte, compatibilidade Android 10–16, imagens muito grandes, memória e latência.
- Nunca fornecer uma certeza falsa de IV em capturas ambíguas. Dados do jogador só são armazenados localmente.
