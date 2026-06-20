package com.asus.platform.service.pagamento;

import com.asus.platform.domain.Plano;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Gateway padrão: ativação manual do plano (sem cobrança externa). A troca de
 * plano é feita por {@code PUT /api/organizacoes/{orgId}/assinatura}.
 *
 * <p>Ativo quando {@code asus.payments.provider=manual} (ou ausente).</p>
 */
@Component
@ConditionalOnProperty(name = "asus.payments.provider", havingValue = "manual", matchIfMissing = true)
public class GatewayManual implements GatewayPagamento {

    @Override
    public Checkout criarCheckout(Long organizacaoId, Plano plano, String urlSucesso, String urlCancelamento) {
        // Sem provedor externo: nada a cobrar; a ativação é manual.
        return new Checkout(null, null, true);
    }

    @Override
    public String provedor() {
        return "manual";
    }
}
