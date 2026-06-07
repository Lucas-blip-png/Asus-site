package com.asus.platform.web.dto;

import com.asus.platform.domain.CampanhaMembro;
import java.time.LocalDateTime;

public record CampanhaMembroResponse(
        Long id, Long campanhaId, Long usuarioId, String papel, LocalDateTime entrouEm) {

    public static CampanhaMembroResponse de(CampanhaMembro m) {
        return new CampanhaMembroResponse(m.getId(), m.getCampanhaId(), m.getUsuarioId(),
                m.getPapel() == null ? null : m.getPapel().name(), m.getEntrouEm());
    }
}
