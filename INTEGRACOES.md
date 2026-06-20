# Integrações externas — só plugar credenciais

As três pendências que dependem de credenciais/infra do cliente já têm o **seam
pronto no código**. O padrão é sempre o modo "sem credencial" (local / manual /
sem login social), então o app roda e os 22 testes passam sem nada configurado.
Para ativar cada integração: adicione a dependência (quando indicado), defina as
variáveis de ambiente e — onde houver — cole a classe de implementação.

| Integração | Padrão (já ativo) | Ativar com |
|-----------|-------------------|-----------|
| Storage de assets | `ArmazenamentoLocal` (disco) | `asus.storage.tipo=s3` + classe `ArmazenamentoS3` |
| Pagamento | `GatewayManual` (no-op) | `asus.payments.provider=stripe` + classe `GatewayStripe` |
| Login Google | desligado | `spring-boot-starter-oauth2-client` + config OAuth2 |

As variáveis estão modeladas em [`.env.example`](.env.example) e mapeadas em
[`application.yml`](asus-platform/src/main/resources/application.yml).

---

## 1. Storage em nuvem (S3 / Cloudflare R2 / GCS)

O `AssetService` já depende da interface
[`ArmazenamentoAssets`](asus-platform/src/main/java/com/asus/platform/service/storage/ArmazenamentoAssets.java).
Hoje o bean ativo é `ArmazenamentoLocal`. Para nuvem:

### 1.1 Dependência (pom.xml do `asus-platform`)
O AWS SDK v2 fala com S3, **Cloudflare R2** e **GCS** (ambos S3-compatíveis via endpoint):

```xml
<dependency>
    <groupId>software.amazon.awssdk</groupId>
    <artifactId>s3</artifactId>
    <version>2.31.6</version>
</dependency>
```

### 1.2 Variáveis (.env)
```
ASUS_STORAGE_TIPO=s3
ASUS_S3_BUCKET=meu-bucket
ASUS_S3_REGION=auto                 # AWS: us-east-1, etc.
ASUS_S3_ENDPOINT=https://<accountid>.r2.cloudflarestorage.com   # AWS S3: deixe vazio
ASUS_S3_ACCESS_KEY=...
ASUS_S3_SECRET_KEY=...
ASUS_S3_PUBLIC_BASE_URL=https://cdn.seu-dominio.com   # opcional (CDN)
```

### 1.3 Implementação (criar o arquivo)
`asus-platform/src/main/java/com/asus/platform/service/storage/ArmazenamentoS3.java`:

```java
package com.asus.platform.service.storage;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

@Component
@ConditionalOnProperty(name = "asus.storage.tipo", havingValue = "s3")
public class ArmazenamentoS3 implements ArmazenamentoAssets {

    private final S3Client s3;
    private final String bucket;

    public ArmazenamentoS3(
            @Value("${asus.storage.s3.bucket}") String bucket,
            @Value("${asus.storage.s3.region:auto}") String region,
            @Value("${asus.storage.s3.endpoint:}") String endpoint,
            @Value("${asus.storage.s3.access-key}") String accessKey,
            @Value("${asus.storage.s3.secret-key}") String secretKey) {
        this.bucket = bucket;
        var builder = S3Client.builder()
                .region(Region.of(region))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(accessKey, secretKey)));
        if (endpoint != null && !endpoint.isBlank()) {
            builder.endpointOverride(URI.create(endpoint)).forcePathStyle(true); // R2/GCS/MinIO
        }
        this.s3 = builder.build();
    }

    @Override
    public String gravar(Long organizacaoId, String nomeOriginal, InputStream conteudo, long tamanho)
            throws IOException {
        String key = organizacaoId + "/" + UUID.randomUUID() + "_" + nomeOriginal;
        s3.putObject(PutObjectRequest.builder().bucket(bucket).key(key).build(),
                RequestBody.fromInputStream(conteudo, tamanho));
        return key; // guardamos a key como storagePath
    }

    @Override
    public byte[] ler(String storagePath) throws IOException {
        ResponseBytes<?> obj = s3.getObjectAsBytes(
                GetObjectRequest.builder().bucket(bucket).key(storagePath).build());
        return obj.asByteArray();
    }

    @Override
    public void apagar(String storagePath) {
        s3.deleteObject(DeleteObjectRequest.builder().bucket(bucket).key(storagePath).build());
    }
}
```

Pronto: com `ASUS_STORAGE_TIPO=s3`, o Spring desativa `ArmazenamentoLocal`
(`matchIfMissing`) e usa o `ArmazenamentoS3`. Nenhuma outra mudança é necessária —
`AssetService` continua igual.

> **Cloudflare R2:** crie um bucket + token R2, use o endpoint
> `https://<accountid>.r2.cloudflarestorage.com` e `region=auto`.
> **GCS:** ative a "interoperability" (chaves HMAC) e use o endpoint
> `https://storage.googleapis.com`.

---

## 2. Gateway de pagamento (Stripe / Mercado Pago)

O seam é a interface
[`GatewayPagamento`](asus-platform/src/main/java/com/asus/platform/service/pagamento/GatewayPagamento.java)
(padrão `GatewayManual`). A ativação do plano em si já existe e é reaproveitada:
`AssinaturaService.definirPlano(orgId, plano)`. O fluxo recomendado é
**checkout → webhook → `definirPlano`**.

### 2.1 Dependência (Stripe)
```xml
<dependency>
    <groupId>com.stripe</groupId>
    <artifactId>stripe-java</artifactId>
    <version>28.2.0</version>
</dependency>
```

### 2.2 Variáveis (.env)
```
ASUS_PAYMENTS_PROVIDER=stripe
ASUS_PAYMENTS_SUCCESS_URL=https://SEU-APP/conta?pago=1
ASUS_PAYMENTS_CANCEL_URL=https://SEU-APP/conta?cancelado=1
STRIPE_SECRET_KEY=sk_live_...
STRIPE_WEBHOOK_SECRET=whsec_...
```

### 2.3 Implementação (criar o arquivo)
`asus-platform/src/main/java/com/asus/platform/service/pagamento/GatewayStripe.java`:

```java
package com.asus.platform.service.pagamento;

import com.asus.platform.domain.Plano;
import com.stripe.Stripe;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "asus.payments.provider", havingValue = "stripe")
public class GatewayStripe implements GatewayPagamento {

    public GatewayStripe(@Value("${asus.payments.stripe.secret-key}") String secretKey) {
        Stripe.apiKey = secretKey;
    }

    @Override
    public Checkout criarCheckout(Long organizacaoId, Plano plano, String urlSucesso, String urlCancelamento) {
        try {
            SessionCreateParams params = SessionCreateParams.builder()
                    .setMode(SessionCreateParams.Mode.SUBSCRIPTION)
                    .setSuccessUrl(urlSucesso)
                    .setCancelUrl(urlCancelamento)
                    .putMetadata("organizacaoId", String.valueOf(organizacaoId))
                    .putMetadata("plano", plano.name())
                    // .addLineItem(... priceId do plano ...)
                    .build();
            Session s = Session.create(params);
            return new Checkout(s.getUrl(), s.getId(), false);
        } catch (Exception e) {
            throw new IllegalStateException("Falha ao criar checkout Stripe: " + e.getMessage(), e);
        }
    }

    @Override
    public String provedor() { return "stripe"; }
}
```

### 2.4 Endpoints (criar o controller)
`PagamentoController` — um para iniciar o checkout e outro para o webhook que
confirma e ativa o plano:

```java
@RestController
@RequestMapping("/api")
public class PagamentoController {
    private final GatewayPagamento gateway;
    private final AssinaturaService assinaturaService;
    @Value("${asus.payments.success-url:}") String sucesso;
    @Value("${asus.payments.cancel-url:}")  String cancel;
    @Value("${asus.payments.stripe.webhook-secret:}") String webhookSecret;

    public PagamentoController(GatewayPagamento g, AssinaturaService a) { this.gateway = g; this.assinaturaService = a; }

    @PostMapping("/organizacoes/{orgId}/checkout")
    public GatewayPagamento.Checkout checkout(@PathVariable Long orgId, @RequestParam Plano plano) {
        return gateway.criarCheckout(orgId, plano, sucesso, cancel);
    }

    // Stripe: valide a assinatura com Webhook.constructEvent(payload, sigHeader, webhookSecret)
    @PostMapping("/webhooks/stripe")
    public ResponseEntity<String> webhook(@RequestBody String payload,
                                          @RequestHeader("Stripe-Signature") String sig) {
        // 1) Webhook.constructEvent(payload, sig, webhookSecret)
        // 2) se "checkout.session.completed": ler metadata organizacaoId/plano
        // 3) assinaturaService.definirPlano(orgId, Plano.valueOf(plano))
        return ResponseEntity.ok("ok");
    }
}
```

Lembre de liberar `/api/webhooks/**` no `SecurityConfig` (`permitAll`).

> **Mercado Pago:** análogo — dependência `com.mercadopago:sdk-java`,
> `ASUS_PAYMENTS_PROVIDER=mercadopago`, crie `GatewayMercadoPago` com
> `@ConditionalOnProperty(havingValue="mercadopago")` usando `MERCADOPAGO_ACCESS_TOKEN`,
> e trate o webhook de `payment` chamando `definirPlano`.

---

## 3. Login social Google (OAuth2)

O Spring Security já está configurado (Fase 7, JWT). Para somar "Entrar com Google":

### 3.1 Dependência
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-oauth2-client</artifactId>
</dependency>
```

### 3.2 Variáveis (.env)
```
ASUS_OAUTH_GOOGLE_ENABLED=true
GOOGLE_CLIENT_ID=...apps.googleusercontent.com
GOOGLE_CLIENT_SECRET=...
```
No Google Cloud Console → *APIs & Services → Credentials → OAuth client ID (Web)*,
e cadastre o redirect: `https://SEU-APP/login/oauth2/code/google`.

### 3.3 Registro do provedor (application.yml)
```yaml
spring:
  security:
    oauth2:
      client:
        registration:
          google:
            client-id: ${GOOGLE_CLIENT_ID:}
            client-secret: ${GOOGLE_CLIENT_SECRET:}
            scope: openid,email,profile
```

### 3.4 Habilitar no SecurityConfig
Dentro do `securityFilterChain`, adicione (guardado pela flag para não exigir a
dependência quando desligado):

```java
@Value("${asus.oauth.google.enabled:false}") boolean googleEnabled;
// ...
if (googleEnabled) {
    http.oauth2Login(o -> o.successHandler(oAuth2SuccessHandler));
}
```

`OAuth2SuccessHandler`: após o login Google, encontre/registre o usuário pelo
e-mail e **emita o mesmo par de tokens JWT** já usado no login normal
(`JwtService` + o fluxo do `AuthService`), redirecionando para o frontend com o
token (ex.: `#access_token=...`). Assim o resto do app continua usando JWT sem
saber que a origem foi o Google.

---

## Resumo

- **Nada quebra sem credenciais:** os padrões (`local`, `manual`, Google off)
  mantêm o app e os testes verdes.
- **Ativar = variáveis + (quando indicado) dependência + 1 classe**; o `seam` no
  código já existe e o resto da aplicação não muda.
- Modelo de variáveis: [`.env.example`](.env.example).
