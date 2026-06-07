package com.asus.platform.web.dto;

/** Tokens emitidos no login/registro/refresh (Fase 7). */
public record AuthResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        long expiresIn,
        UsuarioResponse usuario) {}
