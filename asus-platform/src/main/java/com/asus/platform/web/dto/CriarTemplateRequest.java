package com.asus.platform.web.dto;

import jakarta.validation.constraints.NotBlank;

public record CriarTemplateRequest(
        @NotBlank String tipo,
        @NotBlank String nome,
        String descricao,
        String jsonConteudo,
        Boolean publico,
        Long autorUsuarioId) {}
