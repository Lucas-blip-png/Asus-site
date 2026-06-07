package com.asus.platform.web.dto;

import jakarta.validation.constraints.NotBlank;

/** Registro de consentimento (LGPD). tipo ex: TERMOS, PRIVACIDADE, ANALYTICS. */
public record RegistrarConsentimentoRequest(
        @NotBlank String tipo,
        String versaoDocumento,
        boolean aceito) {}
