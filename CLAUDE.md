# CLAUDE.md — ASUS RPG Platform

Plataforma web de RPG de mesa (sistema homebrew **ASUS**): ficha automática, campanhas, rolagens em tempo real.
Dono do projeto: Lucas (`Lucas-blip-png`). Conversar em **português**.

## Stack e comandos
- Backend: Spring Boot 3.3.5 / Java 21 em `asus-platform/`. Frontend: React 18 + Vite em `frontend/`.
- Imagem Docker única (React vai para `static/` dentro do jar). Deploy no Railway (`railway.json`, healthcheck `/actuator/health`).
- H2 em memória no dev; PostgreSQL com o perfil `postgres` (definido no Dockerfile). Auth JWT stateless (acesso 15 min, refresh 30 dias via `ASUS_JWT_REFRESH_DAYS`).
- **`ASUS_JWT_SECRET` é obrigatório em produção** (o boot falha com o segredo padrão quando `asus.security.enforce=true`).
- Frontend: `cd frontend && npm ci && npm run build`
- Backend: `cd asus-platform && ./mvnw -B -ntp verify` (é o que a CI roda; há testes com Testcontainers, então precisa de Docker)
- API documentada em `/swagger-ui.html`.
- Para testar o jar com a SPA, ela precisa estar em `classpath:/static/` (`SpaConfig`); senão `GET /` retorna 404.

## Regras de trabalho
- Commits assinados como Claude. Terminar a mensagem com as linhas `Co-Authored-By` e `Claude-Session`.
- Abrir PRs sempre como **draft**.
- **Nunca** colar tokens/PATs em arquivo, commit ou saída de comando.
- Não dar `git pull` na `main` de um clone antigo: ela já foi reescrita à força uma vez. Usar `git fetch` e conferir.
- Alterações de conteúdo do jogo vão no `DataSeeder.java`; mudanças de seed precisam se reaplicar em bancos existentes (ver abaixo).

## Como o seed funciona (`config/DataSeeder.java`)
- A cada boot, se o sistema já existe, loga "Seed ja aplicado; pulando." **mas ainda roda** os `safeRefresh` (admin, bestiário, classes, progressão, perícias, itens, habilidades, vitrine). Itens e habilidades oficiais são apagados e recriados; classes e perícias fazem upsert.
- `Raca` **não** está no `safeRefresh`: editar raça no seed não reaplica em base existente.
- `hab(codigo, nome, classeCodigo, tipo, custo, custoTipo, requisito, efeito)` interpreta o texto de `requisito` ("Nível N", "Destreza 10"; o formato "10 Destreza" não casa).
- `arma(...)` grava o crítico completo no formato `margem/xMult` (ex.: `19/x2`, `20/x3`).

## Regras do sistema que NÃO devem ser quebradas
- **Progressão:** níveis 5,15,25,35,45 dão só PV/PM/PE de classe+raça. Níveis 10,20,30,40,50 dão 20 pontos de perícia + 10 de atributo + PV/PM/PE. Níveis 11/21/31/41 dão **só** a habilidade de desbloqueio (trilha, classe secundária, trilha secundária, maestria), sem atributo nem perícia.
- **Trilha não soma atributos nem perícias automaticamente** (`AsusV1Engine`, `PersonagemService.bonusFixosAtributo`).
- **Passivas de classe/trilha** (`Classe.jsonPassiva`) são coisa diferente das **habilidades do tipo passiva** (linhas de `Habilidade`). Não misturar.
- Perícias aceitam valor **negativo**. Não colocar `Math.max(0, ...)` na leitura do campo "outros".
- **Crítico** é uma string só (`critico` em `Ataque`, `ItemJogo`, `ItemPersonagem`) com margem de ameaça + multiplicador. Toda leitura/escrita passa por `frontend/src/critico.js` (`parseCritico`, `fmtCritico`, `rotuloCritico`). O parser aceita `19`, `x3`, `3x`, `18/x2`, `20/3x`. Não reimplementar em outro arquivo.

## Estado atual (verificado ao escrever este arquivo)
- `main` já tem: crítico editável (PR #11), testes + Swagger + README em inglês (PR #12).
- **Flyway ainda NÃO está na `main`.** O PR #13 foi mergeado na branch `feat/testes-swagger-readme`, mas o `pom.xml` da `main` não tem Flyway e o perfil postgres ainda usa `ddl-auto: update`. Conferir se existe PR aberta antes de refazer.

## Pendências (ordem sugerida)
1. **Segurança: as 6 falhas IDOR foram corrigidas na branch `worktree-asus-handoff`** (cobertas por `IdorIntegrationTest`; vale só depois do merge). `/api/me/*` usa o id do JWT; organização exige DONO/ADMIN (membro para listar); convite e `CombateController` passam por `campanhaService.exigirPermissao`; `DELETE /api/bestiario/{id}` só para o criador (`Criatura.criadorId`) ou o dono do site, e criaturas antigas só o dono apaga; `gravar()` descarta diretórios do nome. Sem login (`enforce=false`) nada é bloqueado, como no resto da API. Ainda a revisar: ~11 endpoints mutantes e `GET /api/organizacoes` (lista todas).
2. **Flyway** na `main` (ver acima) e depois `ddl-auto: validate`.
3. **Railway: volume em `/app/uploads`** + `ASUS_UPLOADS_DIR=/app/uploads` (uploads somem a cada deploy). Passo no painel do Railway; alternativa: R2/S3.
4. **"Passos Leves"**: duas habilidades com o mesmo nome (`PASSOS_LEVES`, ativa 10 PE de Ladrão; `PASSOS_LEVES_X`, passiva de Ladrão/Ninja). Perguntar ao Lucas se renomeia, ele ainda não respondeu.
5. **Expectativa de vida das raças:** o campo `expectativaVida` não existe na entidade `Raca`; a aba Livro > Raças já está pronta para exibir. Incluir `raca()` no `safeRefresh` junto.
6. **Ficha no celular:** overflow horizontal de ~97px (tabela de perícias / bloco de identidade).
7. Trocar a mensagem do log do seed ("Seed ja aplicado; pulando.") por algo que diga que o conteúdo oficial é reaplicado.
8. Menor prioridade (`TODO.md`): container rodando como root, `DataSeeder` de ~1700 linhas (externalizar para JSON), code splitting do bundle.
9. Opcional: normalizar crítico já gravado em fichas (`20/3x` → `20/x3`). Não é necessário, o parser lê todos os formatos.

## Armadilhas conhecidas
- Heredoc com `$$` em shell expande; usar delimitador entre aspas (`<<'EOF'`).
- Playwright em ambiente sem `NODE_PATH` exige import ESM por caminho absoluto.
- `pkill` devolve exit 144 e aborta comandos compostos; rodar isolado.
