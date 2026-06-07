package com.asus.platform.web.dto;

import jakarta.validation.constraints.NotNull;

/**
 * Entrada por convite. Por enquanto o usuario e informado no corpo;
 * na Fase 7 (login real) virá do JWT.
 */
public record EntrarCampanhaRequest(@NotNull Long usuarioId) {}
