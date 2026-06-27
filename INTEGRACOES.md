# Integrações externas — já implementadas, só plugar credenciais

As três integrações estão **implementadas no código** e ficam **atrás de flags**
(`@ConditionalOnProperty`). O padrão é sempre o modo sem credencial
(local / manual / login social desligado), então o app roda e os **22 testes
passam** sem nada configurado. As dependências (AWS SDK S3, Stripe, Spring
OAuth2 Client) **já estão no `pom.xml`**. Para ativar: defina as variáveis de
ambiente. Nenhuma classe nova é necessária.

| Integração | Padrão (ativo) | Ativar com | Classe |
|-----------|----------------|-----------|--------|
| Storage de assets | `ArmazenamentoLocal` | `ASUS_STORAGE_TIPO=s3` | `ArmazenamentoS3` |
| Pagamento | `GatewayManual` | `ASUS_PAYMENTS_PROVIDER=stripe` | `GatewayStripe` + `StripeWebhookController` |
| Login Google | desligado | `ASUS_OAUTH_GOOGLE_ENABLED=true` | `GoogleOAuthConfig` + `OAuth2SuccessHandler` |

Variáveis modeladas em [`.env.example`](.env.example) e mapeadas em
[`application.yml`](asus-platform/src/main/resources/application.yml).

---

## 1. Storage em nuvem (S3 / Cloudflare R2 / GCS)

`AssetService` já usa a interface `ArmazenamentoAssets`. Com `ASUS_STORAGE_TIPO=s3`,
o bean ativo passa a ser `ArmazenamentoS3` (AWS SDK v2, S3-compatível). **Nada mais muda.**

```
ASUS_STORAGE_TIPO=s3
ASUS_S3_BUCKET=meu-bucket
ASUS_S3_REGION=auto                 # AWS: us-east-1, etc.
ASUS_S3_ENDPOINT=https://<accountid>.r2.cloudflarestorage.com   # AWS S3: deixe vazio
ASUS_S3_ACCESS_KEY=...
ASUS_S3_SECRET_KEY=...
```

- **Cloudflare R2:** crie bucket + token R2; endpoint `https://<accountid>.r2.cloudflarestorage.com`, `region=auto`.
- **GCS:** ative a *interoperability* (chaves HMAC) e use `https://storage.googleapis.com`.
- Com S3 ativo, o volume de uploads do Railway deixa de ser necessário.

---

## 2. Pagamento (Stripe)

Com `ASUS_PAYMENTS_PROVIDER=stripe`, o `GatewayStripe` cria uma sessão de
Checkout e o `StripeWebhookController` (`/api/webhooks/stripe`) **ativa o plano só
após o pagamento confirmado**.

```
ASUS_PAYMENTS_PROVIDER=stripe
ASUS_PAYMENTS_CURRENCY=brl
ASUS_PAYMENTS_SUCCESS_URL=https://SEU-APP/conta?pago=1
ASUS_PAYMENTS_CANCEL_URL=https://SEU-APP/conta?cancelado=1
STRIPE_SECRET_KEY=sk_live_...
STRIPE_WEBHOOK_SECRET=whsec_...
```

**Fluxo (já implementado):**
1. Frontend chama `POST /api/organizacoes/{orgId}/checkout?plano=PRO` → recebe `url` do Stripe e redireciona.
2. Usuário paga no Stripe.
3. Stripe chama `POST /api/webhooks/stripe` → o handler valida a assinatura e ativa o plano via `AssinaturaService.definirPlano`.

**Configurar no Stripe Dashboard:** *Developers → Webhooks → Add endpoint* →
`https://SEU-APP/api/webhooks/stripe`, evento `checkout.session.completed`; copie o
*Signing secret* para `STRIPE_WEBHOOK_SECRET`.

**Importante (regras de segurança aplicadas):**
- Planos **pagos nunca** são ativados no `/checkout` — só pelo webhook (pagamento real). Apenas `FREE` ativa direto.
- No modo `manual` (padrão), planos pagos são ativados por um admin via `PUT /api/organizacoes/{orgId}/assinatura`.
- O webhook usa `Webhook.constructEvent` (valida HMAC + tolerância de 5 min contra replay); `definirPlano` é idempotente.
- Ajuste os preços em `GatewayStripe.precoCentavos(...)`.

> **Mercado Pago** (alternativa, não incluída): dependência `com.mercadopago:sdk-java`,
> `ASUS_PAYMENTS_PROVIDER=mercadopago`, crie `GatewayMercadoPago` com
> `@ConditionalOnProperty(name="asus.payments.provider", havingValue="mercadopago")`
> usando `MERCADOPAGO_ACCESS_TOKEN`, e um webhook que chame `definirPlano` na confirmação.

---

## 3. Login social Google (OAuth2)

Implementado e desligado por padrão. Com a flag ligada, surge o botão
"Entrar com Google" no login (o frontend consulta `GET /api/auth/config`), e o
`OAuth2SuccessHandler` emite **o mesmo par de tokens JWT** do login normal.

```
ASUS_OAUTH_GOOGLE_ENABLED=true
GOOGLE_CLIENT_ID=...apps.googleusercontent.com
GOOGLE_CLIENT_SECRET=...
```

**Configurar no Google Cloud Console** (*APIs & Services → Credentials → OAuth client ID → Web*):
- **Authorized redirect URI:** `https://SEU-APP/login/oauth2/code/google`
- (Single-service: o backend serve a SPA, então a mesma origem cobre tudo.)

**Nota de segurança:** os tokens chegam ao SPA no *fragmento* da URL
(`#access_token=...`) e são guardados em `localStorage` — **mesma postura que o
login normal** já adota. O frontend faz `location.replace('/')` em seguida para
não deixar o token no histórico navegável. Para endurecer (cookies `HttpOnly`
+ *code-exchange*), seria preciso migrar todo o esquema de auth do app — fica
como evolução futura.

---

## Produção — checklist

- **`ASUS_SECURITY_ENFORCE=true`** (obrigatório): com isso todo `/api/**` exige
  JWT — incluindo `/checkout` e `PUT /assinatura`. (Em dev, `enforce=false` deixa
  a API aberta por design.) As rotas públicas continuam: `auth/{login,register,refresh,config}`,
  `legal`, `sistemas`, `webhooks`, `oauth2`.
- Defina `ASUS_JWT_SECRET` (≥ 32 bytes) e `ASUS_CORS_ORIGINS` com a URL pública.
- Veja o passo a passo de deploy em [`DEPLOY.md`](DEPLOY.md).

## Resumo

- **Nada quebra sem credenciais:** padrões `local`/`manual`/Google-off → app e 22 testes verdes.
- **Ativar = só variáveis de ambiente** (deps e classes já estão no repo).
