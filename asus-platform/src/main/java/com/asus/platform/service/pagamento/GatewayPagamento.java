package com.asus.platform.service.pagamento;

import com.asus.platform.domain.Plano;

/**
 * Abstração do gateway de pagamento (plano, Seção 4.4).
 *
 * <p>Padrão: {@link GatewayManual} (ativação manual, sem cobrança externa). Para
 * Stripe/Mercado Pago, forneça outra implementação ativada por
 * {@code asus.payments.provider=stripe|mercadopago} — ver {@code INTEGRACOES.md}.</p>
 *
 * <p>O fluxo recomendado: o frontend chama o checkout, o provedor processa o
 * pagamento e dispara um webhook; o handler do webhook confirma e chama
 * {@code AssinaturaService.definirPlano(orgId, plano)}.</p>
 */
public interface GatewayPagamento {

    /**
     * Inicia um checkout para o plano e devolve para onde redirecionar o usuário.
     * No modo manual devolve {@code manual=true} e {@code url=null}.
     */
    Checkout criarCheckout(Long organizacaoId, Plano plano, String urlSucesso, String urlCancelamento);

    /** Identificador do provedor ativo (diagnóstico). */
    String provedor();

    /** Resultado da criação de checkout. */
    record Checkout(String url, String referenciaExterna, boolean manual) {}
}
