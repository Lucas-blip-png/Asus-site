package com.asus.platform.service;

import com.asus.platform.domain.Campanha;
import com.asus.platform.domain.Personagem;
import com.asus.platform.repository.CampanhaPersonagemRepository;
import com.asus.platform.repository.CampanhaRepository;
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
    private final CampanhaPersonagemRepository campanhaPersonagemRepository;
    private final CampanhaRepository campanhaRepository;
    private final DonoService donoService;

    public AcessoService(PersonagemRepository personagemRepository,
                         CampanhaPersonagemRepository campanhaPersonagemRepository,
                         CampanhaRepository campanhaRepository,
                         DonoService donoService) {
        this.personagemRepository = personagemRepository;
        this.campanhaPersonagemRepository = campanhaPersonagemRepository;
        this.campanhaRepository = campanhaRepository;
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

    /**
     * Ficha privada: so o DONO do personagem, o MESTRE de uma campanha em que ele esta
     * vinculado, ou o dono/dev do site podem ver/alterar. Outros jogadores: acesso negado.
     */
    public void exigirDonoOuMestrePersonagem(Long personagemId, UsuarioPrincipal principal) {
        if (principal == null || donoService.ehDono(principal.id())) {
            return;
        }
        Long donoId = personagemRepository.findById(personagemId)
                .map(Personagem::getUsuarioId).orElse(null);
        if (donoId == null || donoId.equals(principal.id())) {
            return;
        }
        if (ehMestreDoPersonagem(personagemId, principal.id())) {
            return;
        }
        throw new AcessoNegadoException("Ficha privada: só o dono e o mestre da campanha podem acessar.");
    }

    /** True se o usuario e mestre de alguma campanha em que o personagem esta vinculado. */
    public boolean ehMestreDoPersonagem(Long personagemId, Long usuarioId) {
        if (usuarioId == null) {
            return false;
        }
        return campanhaPersonagemRepository.findByPersonagemId(personagemId).stream()
                .map(cp -> campanhaRepository.findById(cp.getCampanhaId()).orElse(null))
                .anyMatch(c -> c != null && usuarioId.equals(c.getMestreId()));
    }

    /** Ids de personagens vinculados a campanhas em que o usuario e MESTRE. */
    public java.util.Set<Long> personagensDasMinhasMesas(Long usuarioId) {
        java.util.Set<Long> ids = new java.util.HashSet<>();
        if (usuarioId == null) {
            return ids;
        }
        for (Campanha c : campanhaRepository.findAll()) {
            if (usuarioId.equals(c.getMestreId())) {
                campanhaPersonagemRepository.findByCampanhaId(c.getId())
                        .forEach(cp -> ids.add(cp.getPersonagemId()));
            }
        }
        return ids;
    }
}
