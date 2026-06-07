package com.asus.platform.web.dto;

import jakarta.validation.constraints.NotNull;

/** Compra no marketplace. usuarioId virá do JWT na Fase 7. */
public record ComprarRequest(@NotNull Long usuarioId) {}
