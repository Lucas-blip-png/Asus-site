package com.asus.platform.web.dto;

import com.asus.platform.domain.Consentimento;
import java.time.LocalDateTime;

public record ConsentimentoResponse(
        Long id,
        Long usuarioId,
        String tipo,
        String versaoDocumento,
        boolean aceito,
        LocalDateTime criadoEm) {

    public static ConsentimentoResponse de(Consentimento c) {
        return new ConsentimentoResponse(c.getId(), c.getUsuarioId(), c.getTipo(),
                c.getVersaoDocumento(), c.isAceito(), c.getCriadoEm());
    }
}
