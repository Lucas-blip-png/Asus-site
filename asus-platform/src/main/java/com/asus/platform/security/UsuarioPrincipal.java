package com.asus.platform.security;

/** Usuario autenticado (extraido do JWT) guardado no SecurityContext. */
public record UsuarioPrincipal(Long id, String email) {}
