package com.asus.platform.service;

import com.asus.platform.domain.Permissao;
import com.asus.platform.repository.CampanhaPersonagemRepository;
import com.asus.platform.web.dto.CampanhaMembroResponse;
import com.asus.platform.web.dto.CampanhaResponse;
import com.asus.platform.web.dto.EscudoResponse;
import com.asus.platform.web.dto.PersonagemResponse;
import com.asus.platform.web.dto.RolagemResponse;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * Escudo do Mestre (Fase 8): visao consolidada da campanha, edicao de status de
 * qualquer personagem e historico completo de rolagens (incl. ocultas).
 *
 * <p>As acoes exigem permissao quando {@code usuarioId} e informado (Fase 7
 * tornara a checagem obrigatoria via JWT).</p>
 */
@Service
public class EscudoService {

    private final CampanhaService campanhaService;
    private final PersonagemService personagemService;
    private final RolagemService rolagemService;
    private final CampanhaPersonagemRepository campanhaPersonagemRepository;

    public EscudoService(CampanhaService campanhaService,
                         PersonagemService personagemService,
                         RolagemService rolagemService,
                         CampanhaPersonagemRepository campanhaPersonagemRepository) {
        this.campanhaService = campanhaService;
        this.personagemService = personagemService;
        this.rolagemService = rolagemService;
        this.campanhaPersonagemRepository = campanhaPersonagemRepository;
    }

    public EscudoResponse escudo(Long campanhaId, Long usuarioId) {
        if (usuarioId != null) {
            campanhaService.exigirPermissao(campanhaId, usuarioId, Permissao.GERENCIAR_CAMPANHA);
        }
        CampanhaResponse campanha = campanhaService.buscar(campanhaId);
        List<PersonagemResponse> personagens = campanhaPersonagemRepository.findByCampanhaId(campanhaId)
                .stream().map(cp -> personagemService.buscar(cp.getPersonagemId())).toList();
        List<CampanhaMembroResponse> membros = campanhaService.listarMembros(campanhaId);
        List<RolagemResponse> rolagens = rolagemService.listarCompleto(campanhaId);
        return new EscudoResponse(campanha, personagens, membros, rolagens);
    }

    public PersonagemResponse editarStatus(Long campanhaId, Long personagemId, Long usuarioId,
                                           Integer pvAtual, Integer pmAtual, Integer peAtual) {
        if (usuarioId != null) {
            campanhaService.exigirPermissao(campanhaId, usuarioId, Permissao.EDITAR_STATUS);
        }
        if (!campanhaPersonagemRepository.existsByCampanhaIdAndPersonagemId(campanhaId, personagemId)) {
            throw new IllegalArgumentException("Personagem nao esta nesta campanha");
        }
        return personagemService.atualizarStatus(personagemId, pvAtual, pmAtual, peAtual);
    }

    public List<RolagemResponse> rolagensCompletas(Long campanhaId, Long usuarioId) {
        if (usuarioId != null) {
            campanhaService.exigirPermissao(campanhaId, usuarioId, Permissao.VER_ROLAGENS_OCULTAS);
        }
        return rolagemService.listarCompleto(campanhaId);
    }
}
