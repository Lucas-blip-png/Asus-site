package com.asus.platform.web.dto;

import jakarta.validation.constraints.NotBlank;

public record CriarOrganizacaoRequest(
        @NotBlank String nome,
        @NotBlank String slug) {}
