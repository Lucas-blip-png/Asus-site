package com.asus.platform.web.dto;

import com.asus.platform.domain.Assinatura;
import com.asus.platform.domain.Plano;
import java.time.LocalDateTime;

public record AssinaturaResponse(
        Long organizacaoId,
        String plano,
        String status,
        LocalDateTime inicio,
        LocalDateTime fim) {

    public static AssinaturaResponse de(Assinatura a) {
        return new AssinaturaResponse(a.getOrganizacaoId(),
                a.getPlano() == null ? null : a.getPlano().name(),
                a.getStatus(), a.getInicio(), a.getFim());
    }

    /** Quando a org ainda nao tem assinatura formal: reflete o plano atual. */
    public static AssinaturaResponse padrao(Long organizacaoId, Plano plano) {
        return new AssinaturaResponse(organizacaoId,
                plano == null ? Plano.FREE.name() : plano.name(),
                "SEM_ASSINATURA", null, null);
    }
}
