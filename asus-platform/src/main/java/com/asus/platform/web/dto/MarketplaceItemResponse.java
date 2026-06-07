package com.asus.platform.web.dto;

import com.asus.platform.domain.MarketplaceItem;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record MarketplaceItemResponse(
        Long id,
        Long autorUsuarioId,
        String titulo,
        String descricao,
        String tipo,
        BigDecimal preco,
        String moeda,
        boolean gratuito,
        boolean publicado,
        LocalDateTime criadoEm) {

    public static MarketplaceItemResponse de(MarketplaceItem i) {
        return new MarketplaceItemResponse(i.getId(), i.getAutorUsuarioId(), i.getTitulo(),
                i.getDescricao(), i.getTipo(), i.getPreco(), i.getMoeda(),
                i.isGratuito(), i.isPublicado(), i.getCriadoEm());
    }
}
