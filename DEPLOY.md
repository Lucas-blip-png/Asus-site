# Deploy no Railway (1 serviço + PostgreSQL)

Este projeto sobe como **um único serviço**: o `Dockerfile` builda o React, embute
no Spring Boot e roda o `.jar`. O backend serve a API, o WebSocket e a SPA na
**mesma origem** — então não há CORS entre front e back, nem `wss` separado.

> Arquivos que tornam isso pronto: `Dockerfile`, `.dockerignore`, `railway.json`,
> `.env.example`, `SpaConfig.java` (serve a SPA) e `application.yml` lendo `PORT`.

## Pré-requisitos
- Conta no [Railway](https://railway.app) (login com GitHub).
- O repositório no GitHub (já está: `Lucas-blip-png/Asus-site`).

## Passo a passo

### 1. Criar o projeto
1. Railway → **New Project** → **Deploy from GitHub repo** → escolha `Asus-site`.
2. Railway detecta o `Dockerfile` na raiz e usa ele (graças ao `railway.json`).
   - Se ele perguntar o *Root Directory*, deixe **`/`** (a raiz, onde está o Dockerfile).

### 2. Adicionar o banco
3. No mesmo projeto: **New** → **Database** → **PostgreSQL**.
4. Abra o serviço **Postgres** → aba **Variables/Connect** → copie a
   **`DATABASE_URL`** (formato `postgresql://USER:SENHA@HOST:PORTA/BANCO`).

### 3. Variáveis do serviço do app
5. Abra o serviço do **app** (o do Dockerfile) → aba **Variables** → adicione
   (use o `.env.example` como referência):

   | Variável | Valor |
   |---|---|
   | `SPRING_PROFILES_ACTIVE` | `postgres` |
   | `DB_URL` | `jdbc:postgresql://HOST:PORTA/BANCO` *(da DATABASE_URL, com prefixo `jdbc:`)* |
   | `DB_USER` | o `USER` da DATABASE_URL |
   | `DB_PASSWORD` | a `SENHA` da DATABASE_URL |
   | `ASUS_SECURITY_ENFORCE` | `true` |
   | `ASUS_JWT_SECRET` | um segredo aleatório (≥ 32 bytes — ver abaixo) |
   | `ASUS_CORS_ORIGINS` | a URL pública do app (passo 4) |

   > **Dica Railway:** em vez de copiar a senha na mão, você pode referenciar as
   > variáveis do Postgres: `DB_URL=jdbc:postgresql://${{Postgres.PGHOST}}:${{Postgres.PGPORT}}/${{Postgres.PGDATABASE}}`,
   > `DB_USER=${{Postgres.PGUSER}}`, `DB_PASSWORD=${{Postgres.PGPASSWORD}}`.

### 4. Domínio público
6. Serviço do app → **Settings** → **Networking** → **Generate Domain**.
   Copie a URL (ex.: `https://asus-site-production.up.railway.app`).
7. Volte em **Variables** e ponha essa URL em `ASUS_CORS_ORIGINS`. *(Redeploy.)*

### 5. Uploads persistentes (opcional, recomendado)
8. Serviço do app → **Settings** → **Volumes** → **Add Volume**, mount path
   `/app/uploads`. Depois adicione a variável `ASUS_UPLOADS_DIR=/app/uploads`.
   Sem volume, os arquivos enviados somem a cada novo deploy.

### 6. Pronto
9. Abra a URL pública. Login inicial semeado: **`dev@asus.local` / `dev12345`**.
   **Troque/remova esse usuário** antes de divulgar.

## Gerar o `ASUS_JWT_SECRET`

PowerShell:
```powershell
[Convert]::ToBase64String((1..48 | ForEach-Object { Get-Random -Maximum 256 }))
```
Linux/macOS:
```bash
openssl rand -base64 48
```

## Atualizações
Todo `git push` na branch conectada dispara um novo build/deploy automático.

## Notas
- **Banco:** o schema é criado por `ddl-auto: update` (Hibernate). Para um histórico
  de migrações no futuro, dá para plugar Flyway/Liquibase.
- **Memória:** a imagem usa `-XX:MaxRAMPercentage=75` para caber no free/trial.
- **Escala:** o broker WebSocket é em memória — perfeito para **1 instância**.
  Para várias instâncias, troque por um broker externo (ex.: RabbitMQ).
- **Render/Fly.io:** o mesmo `Dockerfile` funciona; só recrie as variáveis acima.
