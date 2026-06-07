package com.asus.platform.web.dto;

import jakarta.validation.constraints.NotBlank;
import java.math.BigDecimal;

/** Preco nulo ou <= 0 marca o item como gratuito. publicado padrao true. */
public record CriarItemMarketplaceRequest(
        @NotBlank String titulo,
        String descricao,
        String tipo,
        BigDecimal preco,
        String moeda,
        Boolean publicado,
        Long autorUsuarioId) {}
