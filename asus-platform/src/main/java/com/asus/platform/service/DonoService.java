package com.asus.platform.service;

import com.asus.platform.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Identifica o dono/dev do site (config {@code asus.admin.email}).
 * O dono tem acesso total: vê todas as campanhas e todos os personagens.
 */
@Service
public class DonoService {

    @Value("${asus.admin.email:dev@asus.local}")
    private String adminEmail;

    private final UsuarioRepository usuarioRepository;

    public DonoService(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    public boolean ehDono(Long usuarioId) {
        if (usuarioId == null || adminEmail == null) {
            return false;
        }
        return usuarioRepository.findById(usuarioId)
                .map(u -> adminEmail.equalsIgnoreCase(u.getEmail()))
                .orElse(false);
    }
}
