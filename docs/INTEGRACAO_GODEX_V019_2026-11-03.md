# PokeGoBR 0.0.19 — Integração inicial com GoDex (godex.site)

**Data:** 03/11/2026. **Base:** PokeGoBR 0.0.18, build #28.
**Site informado pelo usuário como seu outro site:** https://godex.site/

## Verificação pública do site

- Início localizado em português do Brasil: https://godex.site/pt-BR
- Checklist público ShinyDex: https://godex.site/shinydex
- Checklist público Dynamax/Gigantamax: https://godex.site/maxdex
- Coleções públicas por URL https://godex.site/public-collection/<identificador>, por exemplo o identificador público 30fceaa65b9db02479d8042198dd7f28.
- Site apresenta categorias e recursos de coleção: Have/Need (tenho/faltam), Shiny, Hundo, Shundo, Sombroso, Purificado, Sortudo, Mega, Eventos/Fantasias, formas e gênero; coleções públicas/privadas.
- Pesquisa de coleções tem termos similares à pesquisa do Pokémon GO: shiny, shadow, mega, costume, 4*, intervalos numéricos e operadores & , !.
- A página de coleções indica explicitamente que não consegue avaliar filtros internos do jogo como PC, PS, IV por atributo, golpes, amizade ou distância.
- Não foi encontrada documentação pública de API de sincronização de coleções, autenticação OAuth para clientes externos, exportação por endpoint ou CDN de sprites reutilizáveis. A propriedade indicada pelo usuário é uma afirmação dele; não comprova automaticamente direitos sobre todo conteúdo de terceiros eventualmente embutido no site.

## Implementação real no Android

1. **Extras → GoDex:** acesso a início PT-BR, ShinyDex e checklist Max Battles.
2. **Coleções públicas:** campo para colar a URL de coleção, com verificação estrita de prefixo, HTTPS, hostname godex.site e formato do identificador; sem URLs arbitrárias, tokens, query strings ou redirecionamentos configuráveis.
3. **Pokédex:** botão de atalho para GoDex.
4. **Meus Pokémon:** botão de atalho para coleções GoDex.
5. **Privacidade:** Intent ACTION_VIEW abre no navegador externo. Login/cookies permanecem no navegador e não são acessados pelo PokeGoBR; o aplicativo não envia OCR, screenshots, exportações pessoais nem dados do treinador ao site.
6. **Histórico:** scanners, dados de usuários e banco de espécies do PokeGoBR continuam locais; não há sincronização automática de Have/Need nesta versão.
7. **Testes unitários:** URLs aprovadas e bloqueio de domínios similares, HTTP, scripts, URLs de sessão e URLs privadas.

## O que o site ainda NÃO fornece ao scanner

A existência de uma lista visual ou de sprites na página não fornece um motor confiável de IV, nem Game Master do Pokémon GO, nem direitos automáticos para baixar/republicar todas as imagens. Não foi implementado scraping autenticado, WebView com login, cópia em massa de imagens, nem consumo de API não documentada.

## Próximas integrações possíveis (condicionais)

Se o proprietário fornecer código-fonte/endpoints documentados e autorização técnica do backend:
- API versionada GET catálogo de espécies/formas/trajes com IDs GO e informações de origem/licença para sprites.
- OAuth ou autorização curta por conta para acessar coleções privadas, sem capturar cookies do navegador.
- Importação/sincronização opt-in Have/Need e listas de trocas, com deduplicação cuidadosa por espécie/forma/variante, nunca por PC/IV.
- Publicação voluntária de captura IV após revisão, com seleção explícita de conta e coleção; nunca transmitir screenshots pessoais automaticamente.
- Cache local somente de conteúdo e mídia cujo uso esteja permitido e com prazo de atualização definido.

## Build

Aplica todos os patches históricos scanner_v003.py até scanner_v018.py; finalmente scanner_v019.py, que copia GoDexLinks.kt, GoDexLinksTest.kt e UnifiedHubV19.kt, ajusta versionName 0.0.19 e versionCode 19. Workflow compilado em GitHub Actions. O release público contém APK debug de testes e fonte reconstituível sem dados privados.
