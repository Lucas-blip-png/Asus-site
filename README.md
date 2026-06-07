# ASUS RPG Platform

Plataforma comercial de RPG de mesa tendo o **ASUS** como sistema oficial e padrão,
implementada a partir do *plano de implementação v4 comercial*.

O monorepo tem duas partes:

| Pasta | O que é | Stack |
|-------|---------|-------|
| [`asus-platform/`](asus-platform) | Backend / API REST + WebSocket | Java 21, Spring Boot 3.3, JPA, Spring Security + JWT |
| [`frontend/`](frontend) | SPA / VTT web (inclui Overlay OBS) | React + Vite |

O **ASUS** é o sistema oficial: 7 atributos, 13 raças, 16 classes (+ trilhas),
26 perícias, progressão de 50 níveis, itens em T$, habilidades e regras de
construção de feitiços — o conteúdo real extraído dos documentos-fonte.

## Fases / seções do plano implementadas

- **1** Núcleo SaaS (orgs, personagens, ficha automatizada, auditoria, snapshots, export/debug)
- **2** Frontend React (VTT: ficha com heptágono de atributos, barras de PV/PM/PE, perícias)
- **3** Importação, edição com snapshot, histórico de auditoria
- **4** Campanhas, membros, convites, permissões
- **5** Rolagens (NdF+M, crítico/falha, ocultas)
- **6** Tempo real (WebSocket/STOMP)
- **7** Autenticação JWT (login/refresh), Spring Security, CORS, rate limit
- **8** Escudo do Mestre
- **9** Overlay OBS
- **10** Planos e limites + assinatura manual
- **11** Assets (upload local + whitelist de MIME)
- **12** Marketplace e templates
- **13** LGPD (export, consentimento, exclusão/anonimização, termos)
- **16** Analytics de eventos por organização
- **17** Notificações do usuário (lidas/não-lidas)
- **18** Sessões/Calendário (agenda + presença)
- **20.1** Sanitização de HTML em descrições
- **21.1** Membros da organização (renomear, listar/adicionar/remover)

Ficha com **Ataques** (Combate) e **Feitiços** (Magias) por personagem, e um
**Bestiário** de criaturas.

Pendências que dependem de credenciais/infra do cliente: **Google OAuth**, **gateway
de pagamento** e **storage em nuvem** (S3/R2/GCS).

## Rodar tudo

```bash
# 1) Backend (precisa de JDK 21; o Maven vem via wrapper)
cd asus-platform
./mvnw spring-boot:run        # Windows: .\mvnw.cmd spring-boot:run   → http://localhost:8080

# 2) Frontend (precisa de Node 18+), em outro terminal
cd frontend
npm install
npm run dev                   # http://localhost:5173
```

Login dev: **dev@asus.local / dev12345**.

## Deploy (Railway)

O projeto sobe como **um serviço só**: o `Dockerfile` builda o React, embute no
Spring Boot e roda o `.jar` (API + WebSocket + SPA na mesma origem). Passo a passo
completo, variáveis e banco PostgreSQL em **[`DEPLOY.md`](DEPLOY.md)**. Resumo:

1. Railway → *Deploy from GitHub repo* → `Asus-site` (usa o `Dockerfile` da raiz).
2. *New → Database → PostgreSQL*.
3. Variáveis do app (modelo em [`.env.example`](.env.example)):
   `SPRING_PROFILES_ACTIVE=postgres`, `DB_URL/DB_USER/DB_PASSWORD`,
   `ASUS_SECURITY_ENFORCE=true`, `ASUS_JWT_SECRET`, `ASUS_CORS_ORIGINS`.
4. *Generate Domain* → abra a URL. (Opcional: Volume em `/app/uploads`.)

Detalhes e walkthrough de cada parte: veja os READMEs em
[`asus-platform/`](asus-platform/README.md) e [`frontend/`](frontend/README.md).
