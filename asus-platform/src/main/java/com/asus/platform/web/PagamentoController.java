package com.asus.platform.web;

import com.asus.platform.domain.Plano;
import com.asus.platform.service.AssinaturaService;
import com.asus.platform.service.pagamento.GatewayPagamento;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Checkout de plano (plano, Seção 4.4). Provedor-agnóstico: usa o
 * {@link GatewayPagamento} ativo. No modo manual (ou plano FREE) ativa direto;
 * com Stripe devolve a URL de checkout e a ativação vem pelo webhook.
 */
@RestController
@RequestMapping("/api")
public class PagamentoController {

    private final GatewayPagamento gateway;
    private final AssinaturaService assinaturaService;

    @Value("${asus.payments.success-url:}")
    private String successUrl;
    @Value("${asus.payments.cancel-url:}")
    private String cancelUrl;

    public PagamentoController(GatewayPagamento gateway, AssinaturaService assinaturaService) {
        this.gateway = gateway;
        this.assinaturaService = assinaturaService;
    }

    @PostMapping("/organizacoes/{orgId}/checkout")
    public GatewayPagamento.Checkout checkout(@PathVariable Long orgId, @RequestParam Plano plano) {
        // FREE nao tem cobranca: pode ativar direto.
        if (plano == Plano.FREE) {
            assinaturaService.definirPlano(orgId, Plano.FREE);
            return new GatewayPagamento.Checkout(null, null, true);
        }
        // Planos PAGOS NUNCA sao ativados aqui — a ativacao so acontece apos pagamento
        // confirmado: via webhook (Stripe) ou, no modo manual, via PUT /assinatura por um
        // admin. Assim ninguem ativa um plano pago sem pagar.
        return gateway.criarCheckout(orgId, plano, successUrl, cancelUrl);
    }
}
