package com.asus.platform.web.dto;

import com.asus.platform.domain.CampanhaPersonagem;
import java.time.LocalDateTime;

public record CampanhaPersonagemResponse(
        Long id, Long campanhaId, Long personagemId, String personagemNome, LocalDateTime adicionadoEm) {

    public static CampanhaPersonagemResponse de(CampanhaPersonagem cp, String personagemNome) {
        return new CampanhaPersonagemResponse(
                cp.getId(), cp.getCampanhaId(), cp.getPersonagemId(), personagemNome, cp.getAdicionadoEm());
    }
}
