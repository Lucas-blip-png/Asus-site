package com.asus.platform.web.dto;

import com.asus.platform.domain.Usuario;
import java.time.LocalDateTime;

public record UsuarioResponse(
        Long id,
        String nome,
        String email,
        boolean anonimizado,
        LocalDateTime criadoEm) {

    public static UsuarioResponse de(Usuario u) {
        return new UsuarioResponse(u.getId(), u.getNome(), u.getEmail(),
                u.isAnonimizado(), u.getCriadoEm());
    }
}
