package com.asus.platform.web.dto;

import com.asus.platform.domain.Usuario;
import java.time.LocalDateTime;

public record UsuarioResponse(
        Long id,
        String nome,
        String email,
        boolean dono,
        boolean anonimizado,
        LocalDateTime criadoEm) {

    public static UsuarioResponse de(Usuario u) {
        return de(u, false);
    }

    public static UsuarioResponse de(Usuario u, boolean dono) {
        return new UsuarioResponse(u.getId(), u.getNome(), u.getEmail(), dono,
                u.isAnonimizado(), u.getCriadoEm());
    }
}
