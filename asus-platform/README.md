# ASUS RPG Platform — Backend

Backend do **ASUS** conforme o *plano de implementação v4 comercial*. O ASUS é o
sistema oficial e padrão da plataforma; a arquitetura é multi-tenant (organizações),
com regras versionadas (`ASUS_V1`) e engine de regras plugável. O conteúdo do
sistema (atributos, raças, classes, perícias, progressão, itens, habilidades e
regras de feitiço) é o **conteúdo real do ASUS**, extraído dos documentos-fonte.

## Fases / seções implementadas

- **Fase 1** — Núcleo SaaS (Seções 22–24): organizações, personagens, ficha
  automatizada, auditoria, snapshots, export, debug.
- **Fase 3** — Importação de personagem, edição (`PUT`) com recálculo + snapshot
  (`CRIACAO`/`EDICAO`/`LEVEL_UP`) e histórico de auditoria.
- **Fase 4** — Campanhas (Seções 8, 9, 21.4): config, membros por papel, vínculo
  de personagens, convites por código, permissões.
- **Fase 5** — Rolagens (Seção 21.5): expressão `NdF+M`, crítico/falha crítica
  (`ASUS_V1`), rolagem oculta e revelação.
- **Fase 6** — Tempo real (STOMP/WebSocket): rolagens e status ao vivo; presença.
- **Fase 7** — Segurança: registro/login com **JWT** (access + refresh), Spring
  Security, BCrypt, CORS, rate limit no login.
- **Fase 8** — Escudo do Mestre: painel consolidado, edição de status e histórico.
- **Fase 10** — Planos/Assinatura: limites por plano (Seção 4.2) e troca manual.
- **Fase 11** — Assets: upload/download em storage local com cota por plano.
- **Fase 12** — Marketplace e Templates: itens, compras e templates.
- **Fase 13** — LGPD: exportar dados, consentimento, exclusão/anonimização, termos.
- **Seção 16** — **Analytics**: eventos por organização.
- **Seção 17** — **Notificações** do usuário (lidas/não-lidas).
- **Seção 18** — **Sessões/Calendário**: agenda de sessões e presença.
- **Seção 20.1** — **Sanitização** de HTML em descrições + whitelist de MIME no upload.
- **Seção 21.1** — **Membros da organização**: renomear org, listar/adicionar/remover.

Além disso, a ficha tem **Ataques** (aba Combate) e **Feitiços** (aba Magias) por
personagem, e há um **Bestiário** de criaturas.

Pendências externas (precisam de credenciais/infra do cliente): **Google OAuth**,
**gateway de pagamento** real e **storage em nuvem** (S3/R2/GCS).

## O sistema ASUS

- **7 atributos**: Força, Constituição, Destreza, Agilidade, Inteligência,
  Sabedoria, Carisma.
- **Raças** dão PV/PM/PE base; **classes** dão PV/PM/PE, bônus de atributo/perícia
  e passivas; classes têm **trilhas** (subclasses) via `classePaiCodigo`.
- **Perícias** treinadas: o treino de cada perícia tem teto = `2 × atributo-base`.
- **Progressão** de 50 níveis (XP + limites de habilidades/feitiços/bênçãos por nível).
- **Itens** e comércio em **T$**; **habilidades** base e de classe; **regras de
  construção de feitiços** (círculo, alcance, poder, duração).

O `DataSeeder` semeia (idempotente) o sistema `ASUS`, **13 raças**, **26 perícias**,
**16 classes + trilhas**, **50 níveis** de progressão, itens, habilidades, o usuário
dev, a organização padrão e uma campanha inicial. O Bestiário começa vazio e é
preenchido via `POST /api/bestiario`.

## Stack

- Java 21, Spring Boot 3.3
- Spring Web · Spring Data JPA · Bean Validation · Spring WebSocket (STOMP)
- Spring Security + JWT (jjwt)
- H2 em memória (padrão, zero setup) · PostgreSQL via profile `postgres`
- JUnit 5 + MockMvc (+ STOMP client)

## Como rodar

Pré-requisito: **JDK 21**. Maven é opcional — há o **Maven Wrapper** (`mvnw`/`mvnw.cmd`).

```bash
./mvnw spring-boot:run        # Windows: .\mvnw.cmd spring-boot:run
```

A aplicação sobe em `http://localhost:8080`. Usuário **dev** já semeado:
`dev@asus.local` / `dev12345`. Console H2 (opcional): `http://localhost:8080/h2-console`
(JDBC `jdbc:h2:mem:asus`, user `sa`, sem senha).

### Segurança (Fase 7)

Por padrão a API roda em modo dev (`asus.security.enforce=false`): aberta, exceto
`/api/auth/me`. Em produção, defina `ASUS_SECURITY_ENFORCE=true`, `ASUS_JWT_SECRET`
(≥ 32 bytes) e `ASUS_CORS_ORIGINS`.

### Testes / PostgreSQL

```bash
./mvnw test
./mvnw spring-boot:run -Dspring-boot.run.profiles=postgres   # DB_URL/DB_USER/DB_PASSWORD
```

## Fórmulas ASUS V1

Ficam isoladas em `AsusV1Engine` (mudanças futuras viram `ASUS_V2` sem quebrar
fichas antigas — cada ficha guarda seu `rulesetVersion`):

```
atributosFinais = atributosBase + bônus de raça/classe
PV     = raca.pvBase + classe.pv + Constituição * 2
PM     = raca.pmBase + classe.pm + Inteligência * 2
PE     = raca.peBase + classe.pe + Constituição * 2
Defesa = 10 + Agilidade
Carga  = Força * 2
Perícia: treino de 0 a teto, onde teto = 2 × atributo-base da perícia
```

Rolagens (Fase 5): expressão `NdF+M` (ex: `1d20+5`, `2d6`). Num único `d20`, a
face natural **20** é crítico e **1** é falha crítica. A aleatoriedade fica atrás
da interface `Dado`, então os testes injetam resultados determinísticos.

## Walkthrough da API (curl)

```bash
# Catálogo do sistema ASUS
curl localhost:8080/api/sistemas/asus/atributos
curl localhost:8080/api/sistemas/asus/racas
curl localhost:8080/api/sistemas/asus/classes          # ?base=true só as classes-base
curl localhost:8080/api/sistemas/asus/pericias
curl localhost:8080/api/sistemas/asus/habilidades       # ?classe=CAVALEIRO
curl localhost:8080/api/sistemas/asus/itens             # ?categoria=ARMA
curl localhost:8080/api/sistemas/asus/progressao
curl localhost:8080/api/sistemas/asus/feiticos/regras

# Criar personagem (ficha calculada automaticamente; 7 atributos)
curl -X POST localhost:8080/api/organizacoes/1/personagens \
  -H 'Content-Type: application/json' \
  -d '{
        "nome":"Heitor","jogador":"Ana",
        "racaCodigo":"HUMANO","classeCodigo":"CAVALEIRO","nivel":1,
        "atributosBase":{"forca":3,"constituicao":3,"destreza":2,"agilidade":2,
                         "inteligencia":1,"sabedoria":1,"carisma":2},
        "pericias":{"LUTA":2}
      }'

curl localhost:8080/api/personagens/1                    # ficha completa
curl localhost:8080/api/personagens/1/debug              # passo a passo das fórmulas

# Conteúdo da ficha: ataques (Combate) e feitiços (Magias)
curl -X POST localhost:8080/api/personagens/1/ataques \
  -H 'Content-Type: application/json' -d '{"nome":"Espadada","dano":"1d8","critico":"x2"}'
curl localhost:8080/api/personagens/1/ataques
curl -X POST localhost:8080/api/personagens/1/feiticos \
  -H 'Content-Type: application/json' -d '{"nome":"Bola de Fogo","circulo":2,"custoPm":4}'

# Bestiário
curl -X POST localhost:8080/api/bestiario \
  -H 'Content-Type: application/json' -d '{"nome":"Goblin Batedor","nivel":1,"especie":"Goblin","pv":12}'
curl localhost:8080/api/bestiario

# --- Seções 16/17/18/21.1 ---
curl -X POST localhost:8080/api/analytics \
  -H 'Content-Type: application/json' -d '{"evento":"PERSONAGEM_CRIADO","organizacaoId":1}'
curl localhost:8080/api/organizacoes/1/analytics

curl 'localhost:8080/api/me/notificacoes?usuarioId=1'
curl 'localhost:8080/api/me/notificacoes/nao-lidas?usuarioId=1'

curl -X POST localhost:8080/api/campanhas/1/sessoes \
  -H 'Content-Type: application/json' -d '{"titulo":"Sessão 1","inicio":"2026-06-10T20:00:00"}'
curl localhost:8080/api/campanhas/1/sessoes
curl -X POST localhost:8080/api/sessoes/1/presenca \
  -H 'Content-Type: application/json' -d '{"usuarioId":1,"status":"CONFIRMADO"}'

curl localhost:8080/api/organizacoes/1/membros
curl -X POST localhost:8080/api/organizacoes/1/membros \
  -H 'Content-Type: application/json' -d '{"usuarioId":1,"papel":"ADMIN"}'

# --- Fase 7: autenticação ---
curl -X POST localhost:8080/api/auth/login \
  -H 'Content-Type: application/json' -d '{"email":"dev@asus.local","senha":"dev12345"}'
curl localhost:8080/api/auth/me -H "Authorization: Bearer SEU_ACCESS_TOKEN"
```

## Endpoints por fase / seção

| Fase/Seção | Endpoints |
|------|-----------|
| Sistema | `GET /api/sistemas/asus/{atributos,racas,classes,pericias,habilidades,itens,progressao,feiticos/regras}` |
| 1 | `GET/POST /api/organizacoes`, `GET/POST /api/organizacoes/{orgId}/personagens`, `GET /api/personagens/{id}`, `PATCH /api/personagens/{id}/status`, `GET /api/personagens/{id}/{export,debug,snapshots}` |
| 3 | `POST /api/personagens/import`, `PUT /api/personagens/{id}`, `GET /api/personagens/{id}/auditoria` |
| Ficha | `GET/POST /api/personagens/{id}/ataques`, `DELETE /api/ataques/{id}`, `GET/POST /api/personagens/{id}/feiticos`, `DELETE /api/feiticos/{id}` |
| Bestiário | `GET/POST /api/bestiario`, `DELETE /api/bestiario/{id}` |
| 4 | `GET/POST /api/organizacoes/{orgId}/campanhas`, `GET/PUT /api/campanhas/{id}`, `GET/POST /api/campanhas/{id}/personagens`, `GET /api/campanhas/{id}/membros`, `POST /api/campanhas/{id}/convites`, `POST /api/campanhas/entrar/{codigo}` |
| 5 | `GET/POST /api/campanhas/{id}/rolagens`, `POST /api/campanhas/{id}/rolagens/{rolagemId}/revelar` |
| 6 | WebSocket `/ws` (STOMP) · `/topic/campanhas/{id}/rolagens`, `/topic/personagens/{id}/status`, `/topic/campanhas/{id}/presenca` |
| 7 | `POST /api/auth/{register,login,refresh}`, `GET /api/auth/me` |
| 8 | `GET /api/campanhas/{id}/escudo`, `GET /api/campanhas/{id}/escudo/rolagens`, `PATCH /api/campanhas/{id}/escudo/personagens/{pid}/status` |
| 10 | `GET/PUT /api/organizacoes/{orgId}/assinatura`, `GET /api/organizacoes/{orgId}/limites` |
| 11 | `POST/GET /api/organizacoes/{orgId}/assets`, `GET /api/assets/{id}/conteudo`, `DELETE /api/assets/{id}` |
| 12 | `GET /api/marketplace`, `POST /api/marketplace/items`, `GET /api/marketplace/items/{id}`, `POST /api/marketplace/items/{id}/comprar`, `GET/POST /api/organizacoes/{orgId}/templates`, `GET /api/templates/{publicos,{id}}`, `DELETE /api/templates/{id}` |
| 13 | `GET /api/me/export-data`, `POST /api/me/consentimentos`, `DELETE /api/me/delete-account`, `GET /api/legal/{termos,privacidade}` |
| 16 | `POST /api/analytics`, `GET /api/organizacoes/{orgId}/analytics` |
| 17 | `GET /api/me/notificacoes`, `GET /api/me/notificacoes/nao-lidas`, `POST /api/notificacoes`, `POST /api/notificacoes/{id}/lida` |
| 18 | `GET/POST /api/campanhas/{id}/sessoes`, `GET /api/sessoes/{id}/presencas`, `POST /api/sessoes/{id}/presenca` |
| 21.1 | `PUT /api/organizacoes/{id}`, `GET/POST /api/organizacoes/{id}/membros`, `DELETE /api/organizacoes/{id}/membros/{usuarioId}` |

> Autenticação (Fase 7): `/api/auth/me` exige `Authorization: Bearer <token>`.
> Ações sensíveis aceitam `usuarioId` explícito em modo dev e são checadas contra
> `Permissoes`; com `asus.security.enforce=true` todo `/api/**` exige token.

## Estrutura

```
src/main/java/com/asus/platform
├── AsusPlatformApplication.java
├── config/        DataSeeder, WebSocketConfig, SecurityConfig
├── domain/        entidades + enums (Atributo, Atributos, Classe, Personagem,
│                  Ataque, FeiticoPersonagem, Criatura, Sessao, Notificacao,
│                  AnalyticsEvent, Campanha, Rolagem, Asset, Template, ...)
├── repository/    Spring Data JPA
├── engine/        AsusV1Engine, ContextoCalculo, ResultadoCalculo,
│                  PericiaCalculada, GameSystemRegistry, Dado/DadoAleatorio
├── realtime/      RealtimeNotifier, PresencaController, PresenceListener
├── security/      JwtService, JwtAuthFilter, RateLimiter, UsuarioPrincipal
├── service/       Calculo, Personagem, Organizacao, Campanha, Rolagem, Escudo,
│                  Plano, Assinatura, Asset, Marketplace, Template, Lgpd, Auth, ...
├── util/          Sanitizador (limpeza de HTML, Seção 20.1)
└── web/           controllers + DTOs + tratamento de erro
```

## Testes

22 testes (unitários + integração MockMvc + um STOMP de tempo real). O
`NovasSecoesIntegrationTest` fecha as Seções 16/17/18/20.1/21.1 e o conteúdo de
ficha/bestiário ponta a ponta:

```bash
./mvnw test
```

## O que falta (precisa de credenciais/infra externas)

- **Google OAuth** — `spring-boot-starter-oauth2-client` + client id/secret.
- **Gateway de pagamento** (Mercado Pago/Stripe/…) — hoje o plano é manual no banco.
- **Storage em nuvem** (S3/R2/GCS) — hoje os assets vão para `uploads/` local.
- **Hardening de produção** — backups, monitoramento, política legal definitiva.
</content>
