package com.asus.platform.web.dto;

import com.asus.platform.domain.Compra;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record CompraResponse(
        Long id,
        Long usuarioId,
        Long marketplaceItemId,
        BigDecimal valorPago,
        String moeda,
        LocalDateTime compradoEm) {

    public static CompraResponse de(Compra c) {
        return new CompraResponse(c.getId(), c.getUsuarioId(), c.getMarketplaceItemId(),
                c.getValorPago(), c.getMoeda(), c.getCompradoEm());
    }
}
