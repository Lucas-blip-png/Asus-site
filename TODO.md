# Afazeres / Backlog — ASUS RPG

Lista de pendências combinadas (para retomar depois).

## Infraestrutura
- [ ] **#1 — Volume no Railway para uploads persistentes.** Montar volume em `/app/uploads`
  e setar `ASUS_UPLOADS_DIR=/app/uploads`. Sem isso, avatares/mapas/imagens somem a cada
  deploy. (Passo no painel do Railway; alternativa robusta: R2/S3 via `ASUS_STORAGE_TIPO=s3`.)
- [ ] **#2 — Migrations de banco (Flyway).** Hoje usa `ddl-auto=update` (arriscado). Congelar
  o schema atual como V1 e evoluir versionado; trocar `ddl-auto` para `validate`.
- [x] ~~#3 — CI (GitHub Actions) rodando testes + build.~~ Feito.
- [x] ~~#4 — Healthcheck real via Actuator (`/actuator/health`).~~ Feito.

## Conteúdo do jogo
- [ ] **Raças: expectativa de vida.** A entidade `Raca` não tem esse campo. Adicionar
  `expectativaVida` (entidade + helper `raca()` + `refreshRacas()` no seed) e preencher os
  valores das 13 raças (o front na aba Livro > Raças já está pronto para exibir).
  Opcional: incluir tamanho/deslocamento/idioma por raça.

## Higiene (menor prioridade)
- [ ] Container roda como root — criar usuário não-root na imagem final.
- [ ] `DataSeeder.java` (~1700 linhas) — externalizar dados para JSON; incluir "racas" no
  `safeRefresh` (hoje edições de raça não reaplicam em base já existente).
- [ ] Front num único bundle (~373KB) — code splitting por rota com `React.lazy` (ganho pequeno).
