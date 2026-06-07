package com.asus.platform.web.dto;

import com.asus.platform.domain.Convite;
import java.time.LocalDateTime;

public record ConviteResponse(
        Long id,
        Long campanhaId,
        String codigo,
        String papel,
        Integer maxUsos,
        int usos,
        LocalDateTime expiraEm,
        boolean ativo) {

    public static ConviteResponse de(Convite c) {
        return new ConviteResponse(c.getId(), c.getCampanhaId(), c.getCodigo(),
                c.getPapel() == null ? null : c.getPapel().name(),
                c.getMaxUsos(), c.getUsos(), c.getExpiraEm(), c.isAtivo());
    }
}
