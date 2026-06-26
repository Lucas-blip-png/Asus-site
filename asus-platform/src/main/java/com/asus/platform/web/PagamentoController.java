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
        GatewayPagamento.Checkout checkout = gateway.criarCheckout(orgId, plano, successUrl, cancelUrl);
        // Sem cobrança externa (gateway manual) ou plano gratuito: ativa imediatamente.
        if (checkout.manual() || plano == Plano.FREE) {
            assinaturaService.definirPlano(orgId, plano);
        }
        return checkout;
    }
}
