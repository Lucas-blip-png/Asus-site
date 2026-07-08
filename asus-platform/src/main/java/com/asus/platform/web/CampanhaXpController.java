package com.asus.platform.web;

import com.asus.platform.domain.Permissao;
import com.asus.platform.repository.CampanhaPersonagemRepository;
import com.asus.platform.repository.PersonagemRepository;
import com.asus.platform.security.UsuarioPrincipal;
import com.asus.platform.service.CampanhaService;
import com.asus.platform.service.PersonagemService;
import com.asus.platform.web.dto.ProgressoResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/** XP em massa: o mestre distribui XP para todos os personagens da campanha de uma vez. */
@RestController
@RequestMapping("/api/campanhas/{id}/xp")
public class CampanhaXpController {

    private final CampanhaService campanhaService;
    private final CampanhaPersonagemRepository campanhaPersonagemRepository;
    private final PersonagemRepository personagemRepository;
    private final PersonagemService personagemService;

    public CampanhaXpController(CampanhaService campanhaService,
                                CampanhaPersonagemRepository campanhaPersonagemRepository,
                                PersonagemRepository personagemRepository,
                                PersonagemService personagemService) {
        this.campanhaService = campanhaService;
        this.campanhaPersonagemRepository = campanhaPersonagemRepository;
        this.personagemRepository = personagemRepository;
        this.personagemService = personagemService;
    }

    @PostMapping
    public List<Map<String, Object>> darXp(@PathVariable Long id,
                                           @RequestBody Map<String, Object> body,
                                           @AuthenticationPrincipal UsuarioPrincipal principal) {
        if (principal != null) {
            campanhaService.exigirPermissao(id, principal.id(), Permissao.GERENCIAR_CAMPANHA);
        }
        int quantidade = body.get("quantidade") == null ? 0
                : (int) Double.parseDouble(String.valueOf(body.get("quantidade")));
        if (quantidade <= 0) {
            throw new IllegalArgumentException("quantidade de XP deve ser maior que zero");
        }
        List<Map<String, Object>> resultado = new ArrayList<>();
        campanhaPersonagemRepository.findByCampanhaId(id).forEach(cp ->
                personagemRepository.findById(cp.getPersonagemId()).ifPresent(p -> {
                    ProgressoResponse r = personagemService.atualizarProgresso(
                            p.getId(), p.getXpAtual() + quantidade, null);
                    resultado.add(Map.of(
                            "personagemId", p.getId(),
                            "nome", r.personagem().nome(),
                            "xpAtual", r.personagem().xpAtual(),
                            "nivel", r.personagem().nivel(),
                            "subiuNivel", !r.niveisGanhos().isEmpty()));
                }));
        return resultado;
    }
}
