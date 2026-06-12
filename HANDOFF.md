# ASUS RPG Platform — Handoff (estado atual)

Plataforma web de RPG de mesa cujo sistema oficial é o **ASUS** (homebrew que usa os
**itens de Tormenta 20**). Multi-tenant (organizações), com ficha automatizada, campanhas,
rolagens em tempo real, e uma VTT (ficha interativa) no frontend.

## Onde está o código
- **Repo git (raiz do projeto):** `C:\Users\irine\Downloads\Asus rpg site\Asus site\`
  - `asus-platform/` — backend (Spring Boot)
  - `frontend/` — frontend (React + Vite)
- **GitHub (privado):** https://github.com/Lucas-blip-png/Asus-site.git · branch padrão `main`
- **Branch de trabalho:** `feat/asus-real-secoes-16-21`
- **PR aberto (não mergeado):** https://github.com/Lucas-blip-png/Asus-site/pull/3

## Stack
- **Backend:** Java 21, Spring Boot 3.3.5, Spring Web/Data JPA/Validation/Security, WebSocket (STOMP),
  JWT (jjwt 0.12.6), Lombok. Banco padrão **H2 em memória** (reseeda a cada start); profile `postgres` p/ produção.
- **Frontend:** React 18 + Vite 5 + react-router-dom 6 + @stomp/stompjs. Tema dourado/escuro.

## Como rodar / buildar
- **JAVA_HOME:** `C:\Users\irine\.jdks\ms-21.0.10` (Java não está no PATH).
- **Maven:** wrapper `asus-platform\mvnw.cmd` ou standalone `C:\Users\irine\tools\apache-maven-3.9.16\bin\mvn.cmd`.
- **Testes backend:** `mvn -f asus-platform/pom.xml test` → **22 testes verdes**.
- **Build frontend:** dentro de `frontend/` rodar `npm run build` (--prefix não funciona).
- **Dev servers** (configs em `.claude/launch.json`): backend `mvnw.cmd spring-boot:run` (8080), frontend `npm run dev` (5173, faz proxy de /api e /ws p/ 8080).
- **Login dev semeado:** `dev@asus.local` / `dev12345`.

## Regras do sistema ASUS (estado atual, já corrigido)
- Personagem **nasce no nível 1**; **base de atributos = bônus fixos da classe + 5 pontos distribuíveis** (os 5 valem **só na criação**).
- Depois de criado, atributos **livres até o teto do nível** (`limiteAtributo` da tabela de progressão; nível 1 = 4).
- Perícias: **5 pontos de treino** na criação; teto por perícia = **2× atributo**.
- **Sem Defesa** (removida da ficha; criaturas do bestiário mantêm defesa).
- **Deslocamento = 4 + Agilidade/5**. **Carga máx = Força × 2** (em "espaços").
- Fórmulas (engine `AsusV1Engine`): `PV = racaPV + classePv + Con×2`, `PM = racaPM + classePm + Int×2`, `PE = racaPE + classePe + Con×2`.
- Limites: Habilidades = Des/2 · Feitiços = Int/2 · Bênçãos = Sab/2.
- **Trilha (subclasse): habilidades de trilha só liberam a partir do nível 11.**
- 7 atributos: Força, Constituição, Destreza, Agilidade, Inteligência, Sabedoria, Carisma.
- `DataSeeder` (idempotente — pula se o sistema ASUS já existe) semeia: ASUS, 13 raças, 26 perícias, 16 classes + trilhas, 50 níveis de progressão (XP + teto por nível), **catálogo de itens T20**, habilidades, usuário dev, org padrão e 1 campanha.

## O que está implementado
**Núcleo e fases do plano (1–13):** organizações, personagens com ficha calculada, auditoria, snapshots,
export/import, debug; campanhas (membros, convites, permissões); rolagens `NdF+M` (crítico/falha, ocultas);
tempo real STOMP; auth JWT (register/login/refresh, Spring Security, rate limit); Escudo do Mestre;
Overlay OBS; planos/limites + assinatura manual; assets (upload local + whitelist de MIME);
marketplace e templates; LGPD (export/consentimento/exclusão/termos).

**Seções extra:** 16 Analytics · 17 Notificações · 18 Sessões/Calendário · 20.1 Sanitização de HTML · 21.1 Membros da organização.

**Ficha editável (Fase A):** editar atributos (5 pts na criação, teto por nível depois), nível e XP;
**XP sobe de nível automático com popup** (recompensa da tabela + novo teto); editar PV/PM/PE; **foto da ficha** (upload de asset).

**Inventário + itens T20 (Fase B1):** catálogo real de Tormenta 20 (armas simples/marciais/exóticas/fogo,
munições, armaduras/escudos, equipamento — preços em T$, dano/crítico/alcance/espaços). Inventário editável:
adicionar do catálogo ou item próprio, quantidade, equipar, remover; **barra de carga** (máx = Força×2) com alerta de sobrecarga.

**Habilidades (Fase B2a):** escolher habilidades com **pré-requisito** (classe/trilha/GERAL + nível + atributo,
derivados do texto do requisito; **trilha só nível 11**), respeitando o limite (Des/2). Dropdown só mostra as liberadas.

**Perícias "Outros" + trava (Fase B2b):** adicionar perícia vinda de item (nome + atributo), editável/removível,
teto 2× atributo. Enforcement no servidor: criar com >5 pontos de atributo/perícia → 400; editar respeita o teto do nível.

## Endpoints principais (backend, prefixo /api)
- Catálogo: `GET /sistemas/asus/{atributos,racas,classes,pericias,habilidades,itens,progressao,feiticos/regras}`
- Personagem: `GET/POST /organizacoes/{orgId}/personagens`, `GET/PUT /personagens/{id}`, `PATCH /personagens/{id}/status`, `PATCH /personagens/{id}/progresso` (XP→level-up), `GET /personagens/{id}/{export,debug,snapshots,auditoria}`, `POST /personagens/import`
- Ficha: `GET/POST /personagens/{id}/ataques` + `DELETE /ataques/{id}`; idem `/feiticos`; `GET/POST/DELETE /personagens/{id}/inventario` + `do-catalogo/{codigo}` + `PUT/DELETE /inventario/{id}`; `GET /personagens/{id}/habilidades` + `/disponiveis` + `POST` + `DELETE /{codigo}`
- Campanhas/rolagens/escudo/sessões/analytics/notificações/membros: ver READMEs.

## Deploy (pronto, não publicado)
- **1 serviço só:** `Dockerfile` multi-stage builda o React → embute no Spring (`SpaConfig` serve a SPA) → roda o `.jar`.
- `railway.json` (builder DOCKERFILE + healthcheck), `.dockerignore`, `.env.example`, **`DEPLOY.md`** (passo a passo Railway + Postgres).
- `application.yml`: `server.port=${PORT}`, `ASUS_UPLOADS_DIR`, profile `postgres` via `DB_URL/DB_USER/DB_PASSWORD`, segurança via `ASUS_SECURITY_ENFORCE/ASUS_JWT_SECRET/ASUS_CORS_ORIGINS`.

## Pendências / o que falta
- **Mergear o PR #3** na `main`.
- **Bestiário sem seed** de criaturas (não havia fonte oficial; a tela cria/lista/remove via API).
- **Produção:** trocar H2→Postgres, `ASUS_SECURITY_ENFORCE=true`, `ASUS_JWT_SECRET` forte, volume p/ uploads.
- Externos (dependem de credenciais): **Google OAuth**, **gateway de pagamento**, **storage em nuvem** (hoje uploads locais), envio de e-mail (notificações só no banco).
- A trava de teto de atributo hoje é sobre os **pontos da base** (a confirmar se deve ser sobre o **valor final** = base + fixos da classe).
- Migrations (Flyway/Liquibase) — hoje `ddl-auto: update`.

## Gotchas
- Reiniciar o backend reseeda o H2 (necessário p/ ver itens T20 + pré-requisitos de habilidade novos).
- Em PowerShell, caminhos com espaço precisam de aspas; `git commit -m @'...'@` quebra se houver `"` no corpo.
- `mvn`/`java` não estão no PATH — sempre setar `JAVA_HOME` e usar caminho do mvn/mvnw.

## Endpoints completos (prefixo `/api`)
**Catálogo do sistema** (`SistemaController`)
- `GET /sistemas/asus/atributos` · `/racas` · `/classes` (`?base=true`) · `/pericias` · `/habilidades` (`?classe=`) · `/itens` (`?categoria=`) · `/progressao` · `/feiticos/regras`

**Auth** (`AuthController`) — JWT
- `POST /auth/register` · `POST /auth/login` · `POST /auth/refresh` · `GET /auth/me` (exige token)

**Organizações + membros** (`OrganizacaoController`)
- `GET/POST /organizacoes` · `GET/PUT /organizacoes/{id}`
- `GET/POST /organizacoes/{id}/membros` · `DELETE /organizacoes/{id}/membros/{usuarioId}`

**Personagens** (`PersonagemController`)
- `GET/POST /organizacoes/{orgId}/personagens` · `GET/PUT /personagens/{id}`
- `PATCH /personagens/{id}/status` (PV/PM/PE) · `PATCH /personagens/{id}/progresso` (XP → level-up + ganhos)
- `POST /personagens/import` · `GET /personagens/{id}/{export,debug,snapshots,auditoria}`

**Conteúdo da ficha**
- Ataques (`ConteudoPersonagemController`): `GET/POST /personagens/{id}/ataques` · `DELETE /ataques/{id}`
- Feitiços: `GET/POST /personagens/{id}/feiticos` · `DELETE /feiticos/{id}`
- Inventário (`InventarioController`): `GET/POST /personagens/{id}/inventario` · `POST /personagens/{id}/inventario/do-catalogo/{codigo}` · `PUT/DELETE /inventario/{itemId}`
- Habilidades (`HabilidadePersonagemController`): `GET /personagens/{id}/habilidades` · `GET /personagens/{id}/habilidades/disponiveis` · `POST /personagens/{id}/habilidades` (body `{codigo}`) · `DELETE /personagens/{id}/habilidades/{codigo}`

**Bestiário** (`BestiarioController`)
- `GET/POST /bestiario` · `DELETE /bestiario/{id}`

**Campanhas + rolagens + escudo**
- Campanhas (`CampanhaController`): `GET/POST /organizacoes/{orgId}/campanhas` · `GET/PUT /campanhas/{id}` · `GET/POST /campanhas/{id}/personagens` · `GET /campanhas/{id}/membros` · `POST /campanhas/{id}/convites` · `POST /campanhas/entrar/{codigo}`
- Rolagens: `GET/POST /campanhas/{id}/rolagens` · `POST /campanhas/{id}/rolagens/{rolagemId}/revelar`
- Escudo do Mestre: `GET /campanhas/{id}/escudo` · `GET /campanhas/{id}/escudo/rolagens` · `PATCH /campanhas/{id}/escudo/personagens/{pid}/status`
- WebSocket STOMP: endpoint `/ws` · tópicos `/topic/campanhas/{id}/rolagens`, `/topic/personagens/{id}/status`, `/topic/campanhas/{id}/presenca`

**Planos / Assets / Marketplace / LGPD**
- Planos: `GET/PUT /organizacoes/{orgId}/assinatura` · `GET /organizacoes/{orgId}/limites`
- Assets: `POST/GET /organizacoes/{orgId}/assets` · `GET /assets/{id}/conteudo` · `DELETE /assets/{id}`
- Marketplace/Templates: `GET /marketplace` · `POST /marketplace/items` · `GET /marketplace/items/{id}` · `POST /marketplace/items/{id}/comprar` · `GET/POST /organizacoes/{orgId}/templates` · `GET /templates/{publicos,{id}}` · `DELETE /templates/{id}`
- LGPD: `GET /me/export-data` · `POST /me/consentimentos` · `DELETE /me/delete-account` · `GET /legal/{termos,privacidade}`

**Seções 16–18**
- Analytics: `POST /analytics` · `GET /organizacoes/{orgId}/analytics`
- Notificações: `GET /me/notificacoes` · `GET /me/notificacoes/nao-lidas` · `POST /notificacoes` · `POST /notificacoes/{id}/lida`
- Sessões/Calendário: `GET/POST /campanhas/{id}/sessoes` · `GET /sessoes/{id}/presencas` · `POST /sessoes/{id}/presenca`

> Em modo dev (`asus.security.enforce=false`) a API fica aberta exceto `/auth/me`. Em produção (`=true`), todo `/api/**` exige `Authorization: Bearer <token>` (exceto `/auth/login,register,refresh`, `/legal/**`, `/sistemas/**`, `/ws`).

## Deploy — passo a passo (Railway, 1 serviço + Postgres)
O `Dockerfile` builda o React, embute no Spring e roda o `.jar` (API + WebSocket + SPA na mesma origem).
1. **Railway → New Project → Deploy from GitHub repo →** `Asus-site` (detecta o `Dockerfile` da raiz; se pedir *Root Directory*, deixe `/`).
2. **New → Database → PostgreSQL.** Abra o serviço Postgres e copie a `DATABASE_URL` (`postgresql://USER:SENHA@HOST:PORTA/BANCO`).
3. No serviço do **app → Variables** (modelo em `.env.example`):
   - `SPRING_PROFILES_ACTIVE=postgres`
   - `DB_URL=jdbc:postgresql://HOST:PORTA/BANCO` · `DB_USER=USER` · `DB_PASSWORD=SENHA`
     (ou referencie: `DB_URL=jdbc:postgresql://${{Postgres.PGHOST}}:${{Postgres.PGPORT}}/${{Postgres.PGDATABASE}}`, `DB_USER=${{Postgres.PGUSER}}`, `DB_PASSWORD=${{Postgres.PGPASSWORD}}`)
   - `ASUS_SECURITY_ENFORCE=true`
   - `ASUS_JWT_SECRET=<aleatório ≥ 32 bytes>` (gere com `openssl rand -base64 48`)
   - `ASUS_CORS_ORIGINS=https://SEU-APP.up.railway.app`
4. **Settings → Networking → Generate Domain.** Pegue a URL pública e coloque em `ASUS_CORS_ORIGINS` (redeploy).
5. (Recomendado) **Settings → Volumes → Add Volume** em `/app/uploads` + variável `ASUS_UPLOADS_DIR=/app/uploads` (senão os uploads somem a cada deploy).
6. Abra a URL → login `dev@asus.local` / `dev12345` → **troque/remova o usuário dev** antes de divulgar.

- O mesmo `Dockerfile` funciona em **Render/Fly.io** (só recriar as variáveis).
- Cada `git push` na branch conectada dispara novo build/deploy.
- Detalhe completo em `DEPLOY.md`.
