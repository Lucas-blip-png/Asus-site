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
- Commits **sem** assinatura do Claude (sem `Co-Authored-By` nem `Claude-Session`): só a autoria do dono.
- Abrir PRs sempre como **draft**.
- **Nunca** colar tokens/PATs em arquivo, commit ou saída de comando.
- Não dar `git pull` na `main` de um clone antigo: ela já foi reescrita à força uma vez. Usar `git fetch` e conferir.
- Alterações de conteúdo do jogo vão no `DataSeeder.java`; mudanças de seed precisam se reaplicar em bancos existentes (ver abaixo).

## Como o seed funciona (`config/DataSeeder.java`)
- A cada boot, se o sistema já existe, loga "Seed ja aplicado; pulando." **mas ainda roda** os `safeRefresh` (admin, bestiário, classes, progressão, perícias, itens, habilidades, vitrine). Itens e habilidades oficiais são apagados e recriados; classes, perícias e raças fazem upsert (perícias em `PERICIAS_REMOVIDAS` são apagadas).
- `Raca` **está** no `safeRefresh` (upsert por código, mantém o id, porque personagens referenciam `racaId`): editar raça no seed reaplica em base existente. O texto longo vem de `seed/racas-lore.json` e a longevidade de `EXPECTATIVA_VIDA` (ambos no `DataSeeder`/seed).
- `hab(codigo, nome, classeCodigo, tipo, custo, custoTipo, requisito, efeito)` interpreta o texto de `requisito` ("Nível N", "Destreza 10"; o formato "10 Destreza" não casa).
- `arma(...)` grava o crítico completo no formato `margem/xMult` (ex.: `19/x2`, `20/x3`).

## Regras do sistema que NÃO devem ser quebradas
- **Progressão:** níveis 5,15,25,35,45 dão só PV/PM/PE de classe+raça. Níveis 10,20,30,40,50 dão 20 pontos de perícia + 10 de atributo + PV/PM/PE. Níveis 11/21/31/41 dão **só** a habilidade de desbloqueio (trilha, classe secundária, trilha secundária, maestria), sem atributo nem perícia.
- **Trilha não soma atributos nem perícias automaticamente** (`AsusV1Engine`, `PersonagemService.bonusFixosAtributo`).
- **Passivas de classe/trilha** (`Classe.jsonPassiva`) são coisa diferente das **habilidades do tipo passiva** (linhas de `Habilidade`). Não misturar.
- Perícias aceitam valor **negativo**. Não colocar `Math.max(0, ...)` na leitura do campo "outros".
- **Não existe limite de habilidades** (nem slots): habilidades vêm de subir de nível e de outras formas. Só há limite de feitiços e bênçãos. Não recriar `limiteHabilidades`.
- **A ficha nasce sem habilidade escolhida pelo sistema:** criação e level up não concedem linhas de `Habilidade` sozinhas (o jogador escolhe). As passivas de classe/trilha/raça aparecem pelos próprios dados, não por `Habilidade`.
- **Versatilidade Humana** (Humano, nível 20+): pode ter 1 habilidade de fora da própria classe, à escolha (`HabilidadePersonagemController`); a limpeza de habilidades "intrusas" a preserva.
- **São 26 perícias** (as do `PERICIAS.docx`). **Engenharia e Sanidade não existem**; "Magia" chama-se Feitiços (código `MAGIA`). Não citar essas perícias em textos ou bônus.
- **PM/PV/PE atual não sobe junto com o máximo.** O máximo só vira ajuste manual se diferir do automático; valor ≤ 0 volta ao automático.
- **Crítico** é uma string só (`critico` em `Ataque`, `ItemJogo`, `ItemPersonagem`) com margem de ameaça + multiplicador. Toda leitura/escrita passa por `frontend/src/critico.js` (`parseCritico`, `fmtCritico`, `rotuloCritico`). O parser aceita `19`, `x3`, `3x`, `18/x2`, `20/3x`. Não reimplementar em outro arquivo.

## Estado atual (verificado ao escrever este arquivo)
- `main` já tem: crítico editável (PR #11), testes + Swagger + README em inglês (PR #12).
- PRs draft abertos: #16 (correções das 6 falhas IDOR) e #17 (raças, habilidades, perícias, limite de habilidades). Nenhum mesclado ainda.
- **Flyway ainda NÃO está na `main`.** O PR #13 foi mergeado na branch `feat/testes-swagger-readme`, mas o `pom.xml` da `main` não tem Flyway e o perfil postgres ainda usa `ddl-auto: update`. Conferir se existe PR aberta antes de refazer.

## Pendências (ordem sugerida)
1. **Segurança: 6 falhas IDOR críticas.** Foram provadas por exploração local numa versão anterior; **não verifiquei se a `main` atual as corrigiu**, reconferir primeiro:
   `DELETE /api/me/delete-account?usuarioId=X`, `GET /api/me/export-data?usuarioId=X`, `POST /api/organizacoes/{id}/membros`, `POST /api/campanhas/{id}/convites`, os 8 endpoints de `CombateController`, `DELETE /api/bestiario/{id}`. Revisar também ~11 endpoints mutantes e a assimetria de path traversal em `ArmazenamentoLocal.gravar()`.
2. **Flyway** na `main` (ver acima) e depois `ddl-auto: validate`.
3. **Railway: volume em `/app/uploads`** + `ASUS_UPLOADS_DIR=/app/uploads` (uploads somem a cada deploy). Passo no painel do Railway; alternativa: R2/S3.
4. **Redesign do frontend (Stitch):** existe um projeto "ASUS VTT Dashboard" no Stitch, mas as telas geradas inventaram raças, mecânicas de D&D e nomes. Nada foi aplicado no site. Ao regerar, usar só o conteúdo real do livro do dono e conferir cada tela contra ele e o site original antes de portar para o React.
5. **Fichas antigas:** quem foi criado antes mantém as habilidades que o sistema concedia sozinho (remover na ficha) e o máximo de PV/PM/PE congelado (botão "Voltar ao automático").
6. **Ficha no celular:** overflow horizontal de ~97px (tabela de perícias / bloco de identidade).
7. Trocar a mensagem do log do seed ("Seed ja aplicado; pulando.") por algo que diga que o conteúdo oficial é reaplicado.
8. Menor prioridade (`TODO.md`): container rodando como root, `DataSeeder` de ~1700 linhas (externalizar para JSON), code splitting do bundle.
9. Opcional: normalizar crítico já gravado em fichas (`20/3x` → `20/x3`). Não é necessário, o parser lê todos os formatos.

## Armadilhas conhecidas
- Heredoc com `$$` em shell expande; usar delimitador entre aspas (`<<'EOF'`).
- Playwright em ambiente sem `NODE_PATH` exige import ESM por caminho absoluto.
- `pkill` devolve exit 144 e aborta comandos compostos; rodar isolado.
