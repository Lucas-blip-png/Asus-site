package com.asus.platform.web;

import com.asus.platform.domain.Plano;
import com.asus.platform.service.AssinaturaService;
import com.stripe.model.Event;
import com.stripe.model.StripeObject;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Webhook do Stripe: ativa o plano da organização quando o pagamento conclui.
 * Só existe quando {@code asus.payments.provider=stripe}. A rota é liberada no
 * {@code SecurityConfig} ({@code /api/webhooks/**}); a autenticidade vem da
 * assinatura do Stripe ({@code asus.payments.stripe.webhook-secret}).
 */
@RestController
@RequestMapping("/api/webhooks")
@ConditionalOnProperty(name = "asus.payments.provider", havingValue = "stripe")
public class StripeWebhookController {

    private final AssinaturaService assinaturaService;
    private final String webhookSecret;

    public StripeWebhookController(AssinaturaService assinaturaService,
                                   @Value("${asus.payments.stripe.webhook-secret:}") String webhookSecret) {
        this.assinaturaService = assinaturaService;
        this.webhookSecret = webhookSecret;
    }

    @PostMapping("/stripe")
    public ResponseEntity<String> stripe(@RequestBody String payload,
                                         @RequestHeader(value = "Stripe-Signature", required = false) String assinatura) {
        Event event;
        try {
            // constructEvent valida a assinatura HMAC E rejeita eventos fora da
            // tolerancia de tempo padrao (5 min), o que ja barra replays antigos.
            event = Webhook.constructEvent(payload, assinatura, webhookSecret);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("assinatura invalida");
        }
        // definirPlano e idempotente: reprocessar o mesmo evento apenas reativa o
        // mesmo plano (nenhuma escalada/duplicidade). Para auditoria fina de eventos
        // ja processados, persista event.getId() (ver INTEGRACOES.md).
        if ("checkout.session.completed".equals(event.getType())) {
            StripeObject obj = event.getDataObjectDeserializer().getObject().orElse(null);
            if (obj instanceof Session session && session.getMetadata() != null) {
                String orgId = session.getMetadata().get("organizacaoId");
                String plano = session.getMetadata().get("plano");
                if (orgId != null && plano != null) {
                    assinaturaService.definirPlano(Long.valueOf(orgId), Plano.valueOf(plano));
                }
            }
        }
        return ResponseEntity.ok("ok");
    }
}
