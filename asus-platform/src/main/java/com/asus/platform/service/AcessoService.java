package com.asus.platform.service;

import com.asus.platform.domain.Personagem;
import com.asus.platform.repository.PersonagemRepository;
import com.asus.platform.security.UsuarioPrincipal;
import com.asus.platform.web.AcessoNegadoException;
import org.springframework.stereotype.Service;

/**
 * Checagens de autorizacao por dono. Padrao de toda a API:
 * quando NAO ha usuario autenticado (modo aberto de desenvolvimento, {@code enforce=false}),
 * nada e bloqueado; quando ha, exige que seja o dono do recurso (ou o dono/dev da instancia).
 */
@Service
public class AcessoService {

    private final PersonagemRepository personagemRepository;
    private final DonoService donoService;

    public AcessoService(PersonagemRepository personagemRepository, DonoService donoService) {
        this.personagemRepository = personagemRepository;
        this.donoService = donoService;
    }

    /** Exige que o usuario autenticado seja dono do personagem (ou dono/dev). Nao bloqueia se anonimo. */
    public void exigirDonoPersonagem(Long personagemId, UsuarioPrincipal principal) {
        if (principal == null || donoService.ehDono(principal.id())) {
            return;
        }
        Long donoId = personagemRepository.findById(personagemId)
                .map(Personagem::getUsuarioId).orElse(null);
        if (donoId != null && !donoId.equals(principal.id())) {
            throw new AcessoNegadoException("Você não tem permissão sobre este personagem.");
        }
    }

    /** Como {@link #exigirDonoPersonagem}, mas o MESTRE indicado tambem passa (fluxos de campanha). */
    public void exigirDonoPersonagemOuMestre(Long personagemId, Long mestreId, UsuarioPrincipal principal) {
        if (principal != null && mestreId != null && mestreId.equals(principal.id())) {
            return;
        }
        exigirDonoPersonagem(personagemId, principal);
    }
}
