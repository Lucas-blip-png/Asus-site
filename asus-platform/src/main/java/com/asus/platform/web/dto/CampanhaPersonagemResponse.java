package com.asus.platform.web.dto;

import com.asus.platform.domain.CampanhaPersonagem;
import com.asus.platform.domain.Personagem;
import java.time.LocalDateTime;

public record CampanhaPersonagemResponse(
        Long id, Long campanhaId, Long personagemId, String personagemNome,
        String personagemClasse, Long avatarAssetId, int nivel,
        LocalDateTime adicionadoEm) {

    public static CampanhaPersonagemResponse de(CampanhaPersonagem cp, Personagem p, String classe) {
        return new CampanhaPersonagemResponse(
                cp.getId(), cp.getCampanhaId(), cp.getPersonagemId(),
                p != null ? p.getNome() : null,
                classe,
                p != null ? p.getAvatarAssetId() : null,
                p != null ? p.getNivel() : 0,
                cp.getAdicionadoEm());
    }
}
