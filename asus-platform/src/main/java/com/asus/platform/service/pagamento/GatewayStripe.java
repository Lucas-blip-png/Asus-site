package com.asus.platform.service.pagamento;

import com.asus.platform.domain.Plano;
import com.stripe.Stripe;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Gateway Stripe (Checkout). Ativo quando {@code asus.payments.provider=stripe}.
 *
 * <p>Cria uma sessão de Checkout com o preço do plano e devolve a URL para
 * redirecionar o usuário. A ativação do plano acontece via webhook
 * ({@code checkout.session.completed}) — ver {@link com.asus.platform.web.StripeWebhookController}.</p>
 */
@Component
@ConditionalOnProperty(name = "asus.payments.provider", havingValue = "stripe")
public class GatewayStripe implements GatewayPagamento {

    private final String currency;

    public GatewayStripe(@Value("${asus.payments.stripe.secret-key}") String secretKey,
                         @Value("${asus.payments.currency:brl}") String currency) {
        Stripe.apiKey = secretKey;
        this.currency = currency;
    }

    @Override
    public Checkout criarCheckout(Long organizacaoId, Plano plano, String urlSucesso, String urlCancelamento) {
        try {
            SessionCreateParams params = SessionCreateParams.builder()
                    .setMode(SessionCreateParams.Mode.PAYMENT)
                    .setSuccessUrl(naoVazio(urlSucesso, "https://example.com/sucesso"))
                    .setCancelUrl(naoVazio(urlCancelamento, "https://example.com/cancelado"))
                    .putMetadata("organizacaoId", String.valueOf(organizacaoId))
                    .putMetadata("plano", plano.name())
                    .addLineItem(SessionCreateParams.LineItem.builder()
                            .setQuantity(1L)
                            .setPriceData(SessionCreateParams.LineItem.PriceData.builder()
                                    .setCurrency(currency)
                                    .setUnitAmount(precoCentavos(plano))
                                    .setProductData(SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                            .setName("ASUS RPG — Plano " + plano.name())
                                            .build())
                                    .build())
                            .build())
                    .build();
            Session session = Session.create(params);
            return new Checkout(session.getUrl(), session.getId(), false);
        } catch (Exception e) {
            throw new IllegalStateException("Falha ao criar checkout Stripe: " + e.getMessage(), e);
        }
    }

    @Override
    public String provedor() {
        return "stripe";
    }

    /** Preço do plano em centavos (ajuste conforme sua tabela comercial). */
    private long precoCentavos(Plano plano) {
        return switch (plano) {
            case FREE -> 0L;
            case PRO -> 1990L;
            case MESTRE -> 3990L;
            case GUILD -> 9990L;
        };
    }

    private static String naoVazio(String v, String fallback) {
        return (v == null || v.isBlank()) ? fallback : v;
    }
}
