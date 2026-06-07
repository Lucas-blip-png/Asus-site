# ASUS RPG Platform — Backend

Backend do ASUS conforme o **plano de implementação v4 comercial**. Todas as
fases de backend do plano estão implementadas e cobertas por testes:

- **Fase 1** — Núcleo SaaS mínimo (Seções 22–24): organizações, personagens,
  ficha automatizada, auditoria, snapshots, export, debug.
- **Fase 3** — **Importação** de personagem, **edição** (`PUT`) com recálculo +
  snapshot `LEVEL_UP`/`EDICAO`, e histórico de auditoria por personagem.
- **Fase 4** — **Campanhas** (Seções 8, 9, 21.4): config, membros por papel,
  vínculo de personagens, convites por código, permissões.
- **Fase 5** — **Rolagens** (Seção 21.5): expressão `NdF+M`, crítico/falha
  crítica (`ASUS_V1`), rolagem oculta e revelação.
- **Fase 6** — **Tempo real** (STOMP/WebSocket): rolagens e status transmitidos
  ao vivo; presença.
- **Fase 7** — **Segurança**: registro/login com **JWT** (access + refresh),
  Spring Security, BCrypt, CORS, rate limit no login.
- **Fase 8** — **Escudo do Mestre**: painel consolidado, edição de status e
  histórico completo de rolagens.
- **Fase 10** — **Planos/Assinatura**: limites por plano (Seção 4.2) e troca
  manual de plano.
- **Fase 11** — **Assets**: upload/download em storage local com cota por plano.
- **Fase 12** — **Marketplace e Templates**: itens, compras e templates.
- **Fase 13** — **LGPD**: exportar dados, consentimento, exclusão/anonimização,
  documentos legais.

O ASUS é o sistema oficial e padrão da plataforma. A arquitetura é multi-tenant
(organizações), com regras versionadas (`ASUS_V1`) e engine de regras plugável.
O frontend React (Fases 2 e 9, incl. Overlay OBS) está em [`../frontend`](../frontend).

Pendências externas (precisam de credenciais/infra do cliente): **Google OAuth**
(client id/secret), **gateway de pagamento** real e **storage em nuvem** (S3/R2/GCS).

## Stack

- Java 21, Spring Boot 3.3
- Spring Web · Spring Data JPA · Bean Validation · Spring WebSocket (STOMP)
- Spring Security + JWT (jjwt)
- H2 em memória (padrão, zero setup) · PostgreSQL via profile `postgres`
- JUnit 5 + MockMvc (+ STOMP client)

## Como rodar

Pré-requisito: **JDK 21**. Maven é opcional — o projeto inclui o **Maven Wrapper**
(`mvnw`/`mvnw.cmd`), que baixa o Maven na primeira execução.

```bash
# na raiz do projeto (use ./mvnw no Linux/macOS, mvnw.cmd no Windows)
./mvnw spring-boot:run
```

(Se você já tem Maven 3.9+ instalado, pode usar `mvn` no lugar de `./mvnw`.)

A aplicação sobe em `http://localhost:8080`. Ao iniciar, o `DataSeeder` cria:
o sistema ASUS_V1, raças (HUMANO, ANAO, ELFO), classes (GUERREIRO, MAGO, LADINO),
perícias, um usuário dev, a **organização padrão** (`slug = asus-oficial`) e uma
**campanha inicial** (id 1, com o dev como mestre).

Usuário **dev** já vem semeado para login: `dev@asus.local` / `dev12345`.

Console do H2 (opcional): `http://localhost:8080/h2-console`
(JDBC URL `jdbc:h2:mem:asus`, user `sa`, sem senha).

### Segurança (Fase 7)

Por padrão a API roda em modo dev (`asus.security.enforce=false`): aberta, exceto
`/api/auth/me` que exige token. Em produção, defina `ASUS_SECURITY_ENFORCE=true`
(exige JWT em todo `/api/**`), `ASUS_JWT_SECRET` (≥ 32 bytes) e `ASUS_CORS_ORIGINS`.
Google OAuth pode ser plugado depois via `spring-boot-starter-oauth2-client`.

### Rodar os testes

```bash
./mvnw test
```

### Usar PostgreSQL

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=postgres
# variáveis opcionais: DB_URL, DB_USER, DB_PASSWORD
```

## Walkthrough da API (curl)

```bash
# Listar organizações (a padrão "ASUS Oficial" já vem semeada)
curl localhost:8080/api/organizacoes

# Ver o sistema ASUS e suas regras
curl localhost:8080/api/sistemas/asus
curl localhost:8080/api/sistemas/asus/racas
curl localhost:8080/api/sistemas/asus/classes
curl localhost:8080/api/sistemas/asus/pericias

# Criar uma organização nova (opcional)
curl -X POST localhost:8080/api/organizacoes \
  -H 'Content-Type: application/json' \
  -d '{"nome":"Mesa da Sexta","slug":"mesa-sexta"}'

# Criar personagem na organização 1 (ficha é calculada automaticamente)
curl -X POST localhost:8080/api/organizacoes/1/personagens \
  -H 'Content-Type: application/json' \
  -d '{
        "nome":"Thorin",
        "jogador":"Ana",
        "racaCodigo":"HUMANO",
        "classeCodigo":"GUERREIRO",
        "nivel":1,
        "atributosBase":{"forca":5,"agilidade":3,"vigor":4,"intelecto":2,"presenca":2}
      }'

# Ver ficha completa
curl localhost:8080/api/personagens/1

# Listar personagens da organização
curl localhost:8080/api/organizacoes/1/personagens

# Atualizar status (ex: tomou dano) — gera auditoria
curl -X PATCH localhost:8080/api/personagens/1/status \
  -H 'Content-Type: application/json' -d '{"pvAtual":10}'

# Exportar em JSON
curl localhost:8080/api/personagens/1/export

# Debug do cálculo (passo a passo das fórmulas)
curl localhost:8080/api/personagens/1/debug

# Snapshots da ficha
curl localhost:8080/api/personagens/1/snapshots

# --- Fase 3: editar, histórico e importar ---

# Editar personagem (subir de nível recalcula e gera snapshot LEVEL_UP)
curl -X PUT localhost:8080/api/personagens/1 \
  -H 'Content-Type: application/json' -d '{"nivel":2}'

# Histórico de auditoria do personagem
curl localhost:8080/api/personagens/1/auditoria

# Importar personagem (mesmo envelope do /export)
curl -X POST localhost:8080/api/personagens/import \
  -H 'Content-Type: application/json' \
  -d '{
        "exportVersion":"1.0","system":"ASUS","rulesetVersion":"ASUS_V1",
        "personagem":{
          "organizacaoId":1,"nome":"Cópia da Ana","racaCodigo":"HUMANO",
          "classeCodigo":"MAGO","nivel":1,
          "atributosBase":{"forca":1,"agilidade":2,"vigor":2,"intelecto":5,"presenca":3}
        }
      }'

# --- Fase 4: campanhas ---

# Criar campanha na organização 1 (mestre = usuário dev id 1)
curl -X POST localhost:8080/api/organizacoes/1/campanhas \
  -H 'Content-Type: application/json' \
  -d '{"nome":"A Queda de Valdoria","mestreId":1,
       "config":{"usarBencoes":true,"rolagemOcultaPermitida":true}}'

# Adicionar personagem 1 à campanha 1
curl -X POST localhost:8080/api/campanhas/1/personagens \
  -H 'Content-Type: application/json' -d '{"personagemId":1}'

# Criar convite e entrar pelo código retornado
curl -X POST localhost:8080/api/campanhas/1/convites \
  -H 'Content-Type: application/json' -d '{"papel":"JOGADOR"}'
curl -X POST localhost:8080/api/campanhas/entrar/SEUCODIGO \
  -H 'Content-Type: application/json' -d '{"usuarioId":1}'

# Membros e personagens da campanha
curl localhost:8080/api/campanhas/1/membros
curl localhost:8080/api/campanhas/1/personagens

# --- Fase 5: rolagens ---

# Rolar 1d20+5 (crítico em 20 natural, falha crítica em 1)
curl -X POST localhost:8080/api/campanhas/1/rolagens \
  -H 'Content-Type: application/json' \
  -d '{"expressao":"1d20+5","rotulo":"Ataque","personagemId":1}'

# Rolagem oculta do mestre (some do histórico até revelar)
curl -X POST localhost:8080/api/campanhas/1/rolagens \
  -H 'Content-Type: application/json' \
  -d '{"expressao":"1d20","rotulo":"Emboscada","oculta":true,"usuarioId":1}'

# Histórico (ocultas vêm mascaradas) e revelação pelo mestre (usuário 1)
curl localhost:8080/api/campanhas/1/rolagens
curl -X POST 'localhost:8080/api/campanhas/1/rolagens/2/revelar?usuarioId=1'

# --- Fase 7: autenticação (JWT) ---

# Login do usuário dev -> retorna accessToken/refreshToken
curl -X POST localhost:8080/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"email":"dev@asus.local","senha":"dev12345"}'

# Usar o token em rota protegida
curl localhost:8080/api/auth/me -H "Authorization: Bearer SEU_ACCESS_TOKEN"

# --- Fase 10/11/12/13 ---
curl localhost:8080/api/organizacoes/1/limites
curl -X PUT localhost:8080/api/organizacoes/1/assinatura \
  -H 'Content-Type: application/json' -d '{"plano":"PRO"}'
curl -X POST localhost:8080/api/organizacoes/1/assets -F file=@avatar.png -F tipo=AVATAR_PERSONAGEM
curl localhost:8080/api/marketplace
curl localhost:8080/api/campanhas/1/escudo?usuarioId=1   # Escudo do Mestre (Fase 8)
curl localhost:8080/api/legal/privacidade
curl 'localhost:8080/api/me/export-data?usuarioId=1'     # LGPD (Fase 13)
```

## Fórmulas ASUS V1

Como o ASUS é um sistema customizado, estas fórmulas são um **padrão inicial**
e ficam isoladas em `AsusV1Engine`. Mudanças futuras viram `ASUS_V2` sem quebrar
fichas antigas (cada ficha guarda seu `rulesetVersion`).

```
atributosFinais = atributosBase + bônus raciais (raca.jsonHabilidades)
PV     = raca.pvBase + classe.multiplicadorPv * nível + vigor     * 2
PM     = raca.pmBase + classe.multiplicadorPm * nível + intelecto * 2
PE     = raca.peBase + classe.multiplicadorPe * nível + agilidade
Defesa = 10 + agilidade
Perícia = atributoFinal(atributoBase) + (nível / 2)
```

Rolagens (Fase 5): expressão `NdF+M` (ex: `1d20+5`, `2d6`, `d8-1`). Em um único
`d20`, a face natural **20** é acerto crítico e **1** é falha crítica
(`AsusV1Engine.D20_CRITICO`/`D20_FALHA_CRITICA`). A fonte de aleatoriedade fica
atrás da interface `Dado`, então os testes injetam resultados determinísticos.

## Critérios de aceite do MVP (Seção 24) — onde cada um está

| # | Critério | Onde |
|---|----------|------|
| 1 | Criar organização padrão | `DataSeeder` (slug `asus-oficial`) + `POST /api/organizacoes` |
| 2 | Criar personagem ASUS | `POST /api/organizacoes/{orgId}/personagens` |
| 3 | Calcular ficha automaticamente | `CalculoService` + `AsusV1Engine` |
| 4 | Listar personagens da organização | `GET /api/organizacoes/{orgId}/personagens` |
| 5 | Ver ficha completa | `GET /api/personagens/{id}` |
| 6 | Exportar em JSON | `GET /api/personagens/{id}/export` |
| 7 | Registrar auditoria básica | `AuditoriaService` (criação + alteração de status) |
| 8 | Criar snapshot na criação | `SnapshotService` (motivo `CRIACAO`) |
| 9 | Mostrar debug de cálculo | `GET /api/personagens/{id}/debug` |
| 10 | Estrutura pronta p/ planos e permissões | enums `Plano`, `PapelOrganizacao`, `OrganizacaoMembro`, `Permissao`, `Permissoes` |

## Endpoints por fase

| Fase | Endpoints |
|------|-----------|
| 1 | `GET/POST /api/organizacoes`, `GET /api/sistemas/asus/*`, `GET/POST /api/organizacoes/{orgId}/personagens`, `GET /api/personagens/{id}`, `PATCH /api/personagens/{id}/status`, `GET /api/personagens/{id}/{export,debug,snapshots}` |
| 3 | `POST /api/personagens/import`, `PUT /api/personagens/{id}`, `GET /api/personagens/{id}/auditoria` |
| 4 | `GET/POST /api/organizacoes/{orgId}/campanhas`, `GET/PUT /api/campanhas/{id}`, `GET/POST /api/campanhas/{id}/personagens`, `GET /api/campanhas/{id}/membros`, `POST /api/campanhas/{id}/convites`, `POST /api/campanhas/entrar/{codigo}` |
| 5 | `GET/POST /api/campanhas/{id}/rolagens`, `POST /api/campanhas/{id}/rolagens/{rolagemId}/revelar` |
| 6 | WebSocket `/ws` (STOMP) · tópicos `/topic/campanhas/{id}/rolagens`, `/topic/personagens/{id}/status`, `/topic/campanhas/{id}/presenca` |
| 7 | `POST /api/auth/{register,login,refresh}`, `GET /api/auth/me` |
| 8 | `GET /api/campanhas/{id}/escudo`, `GET /api/campanhas/{id}/escudo/rolagens`, `PATCH /api/campanhas/{id}/escudo/personagens/{pid}/status` |
| 10 | `GET/PUT /api/organizacoes/{orgId}/assinatura`, `GET /api/organizacoes/{orgId}/limites` |
| 11 | `POST/GET /api/organizacoes/{orgId}/assets`, `GET /api/assets/{id}/conteudo`, `DELETE /api/assets/{id}` |
| 12 | `GET /api/marketplace`, `POST /api/marketplace/items`, `GET /api/marketplace/items/{id}`, `POST /api/marketplace/items/{id}/comprar`, `GET/POST /api/organizacoes/{orgId}/templates`, `GET /api/templates/{publicos,{id}}`, `DELETE /api/templates/{id}` |
| 13 | `GET /api/me/export-data`, `POST /api/me/consentimentos`, `DELETE /api/me/delete-account`, `GET /api/legal/{termos,privacidade}` |

> Autenticação (Fase 7): `/api/auth/me` exige `Authorization: Bearer <token>`.
> Ações sensíveis (convite, revelar oculta, escudo) ainda aceitam `usuarioId`
> explícito em modo dev e são checadas contra `Permissoes`; com
> `asus.security.enforce=true` todo `/api/**` exige token.

## Estrutura

```
src/main/java/com/asus/platform
├── AsusPlatformApplication.java
├── config/        DataSeeder, WebSocketConfig, SecurityConfig
├── domain/        entidades + enums + embeddables (Campanha, Rolagem, Assinatura,
│                  Asset, MarketplaceItem, Template, Consentimento, Permissoes...)
├── repository/    Spring Data JPA
├── engine/        GameSystemEngine, AsusV1Engine, GameSystemRegistry,
│                  Dado/DadoAleatorio, ExpressaoDado/ResultadoDado
├── realtime/      RealtimeNotifier, PresencaController, PresenceListener
├── security/      JwtService, JwtAuthFilter, RateLimiter, UsuarioPrincipal
├── service/       Calculo, Personagem, Organizacao, Campanha, Rolagem, Escudo,
│                  Plano, Assinatura, Asset, Marketplace, Template, Lgpd, Auth, ...
└── web/           controllers + DTOs + tratamento de erro
```

## Testes

20 testes (unitários + integração MockMvc + um STOMP de tempo real):

```bash
./mvnw test
```

## O que falta (precisa de credenciais/infra externas)

- **Google OAuth** — adicionar `spring-boot-starter-oauth2-client` + client id/secret.
- **Gateway de pagamento** (Mercado Pago/Stripe/…) — hoje o plano é manual no banco.
- **Storage em nuvem** (S3/R2/GCS) — hoje os assets vão para `uploads/` local.
- **Hardening de produção** — backups, monitoramento, política legal definitiva.
