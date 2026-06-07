package com.asus.platform.web.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Pedido de rolagem. {@code expressao} no formato NdF+M (ex: 1d20+5).
 * {@code oculta=true} esconde o resultado dos demais ate o mestre revelar.
 */
public record RolarRequest(
        @NotBlank String expressao,
        String rotulo,
        Long personagemId,
        Long usuarioId,
        boolean oculta) {}
