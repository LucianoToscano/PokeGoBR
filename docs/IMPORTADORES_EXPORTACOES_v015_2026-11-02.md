# PokeGoBR 0.0.15 — Integração de exportações de coleção/IV/PvP

Data de referência: 02/11/2026. Base: v0.0.14 build #21.
Arquivos privados recebidos: exportação CSV Poke Genie, histórico CSV Calcy IV, PGSStats.zip contendo PGSStats.json e PGSData.dat (Java Object Serialization).

## O que foi implementado

- **Meus Pokémon**: importação local via seletor Android de arquivos CSV, ZIP ou JSON.
- **Poke Genie CSV**: identifica cabeçalhos de espécie (Pokemon, Nome), PC, Atq IV, Def IV, PS IV e ranks de fonte Class # (G/U/L). Os ranks são rotulados GL/UL/LL; L não é a Liga Mestra.
- **Calcy IV CSV**: identifica Nr, Name, CP, ØATT IV, ØDEF IV, ØHP IV e intervalos GL/UL/LL Rank (min/max). Médias fracionárias não são arredondadas para um IV exato.
- **PGSStats.json e .zip**: usa pokemons, pokemonId, cp, individualAttack, individualDefense, individualStamina. Ignora entradas marcadas isEgg e identificadores ausentes; resolve o nome via cache local de espécies quando disponível, senão exibe #id.
- **Origem preservada**: importações e histórico scanner separados em áreas privadas de armazenamento Android; importar um CSV não apaga o anterior de outra origem. Contagens são de registros, **não de Pokémon únicos**, já que exportações sobrepostas podem representar o mesmo indivíduo.
- **PvP**: exibe rankings dos arquivos separados por GL/UL/LL e nome da fonte, com aviso de desatualização e ausência de recalculadora por Game Master.
- **Segurança**: o importador não persiste nem publica dados de conta, treinador, localização, apelidos, tokens, presentes, itens, histórico bruto ou arquivos originais.
- **Arquivo Java serializado (.dat)**: reconhece assinatura AC ED 00 05, recusa importação e fornece instrução de usar JSON ou ZIP. Nunca passa o DAT para ObjectInputStream.
- **Limites**: entrada e descompressão de até 12 MB, máximo 20 entradas ZIP, no máximo 6000 registros CSV, limite de 100 colunas e 2048 caracteres por campo. Não grava ou extrai arquivos de ZIP para caminhos locais.
- **Testes**: arquivos sintéticos sem dados pessoais cobrem CSVs com aspas e vírgulas, nomes, IVs, rankings e faixas, PGSStats com ovo e dados privados, ZIP permitido, CSV e DAT inválidos.

## O que ainda falta

- Importação binária segura do formato PGSData.dat (esquema Java e serialização sujeita a mudanças).
- Correspondência única entre os três históricos e a conta PGSStats. Não fazer deduplicação cega por espécie/PC/IV.
- Banco unificado Room com histórico, controle de versões e atualização dos Game Masters GO.
- Reprocessamento de captura com precisão comparável a aplicativos de referência; ranking PvP GO calculado dentro do app.
- Testes no dispositivo real e validação de importação dos arquivos completos.

## Arquitetura para continuidade

Após a sequência base scanner_v003.py até scanner_v014.py, aplicar scanner_v015.py, que copia HubImportV15.kt, UnifiedHubV15.kt (sobre o UnifiedHub.kt final) e HubImportV15Test.kt, atualizando versionCode 15 e versionName 0.0.15.
A CI executa testes unitários e compila APK debug, publicando fonte editável completa + checksums.

**Regra de privacidade:** não colocar CSV/ZIP/DAT/JSON reais nos commits, nos artefatos ou no GitHub público.
