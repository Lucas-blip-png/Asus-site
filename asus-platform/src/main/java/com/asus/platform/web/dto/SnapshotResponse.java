package com.asus.platform.web.dto;

import com.asus.platform.domain.PersonagemSnapshot;
import java.time.LocalDateTime;

public record SnapshotResponse(Long id, Long personagemId, String motivo, LocalDateTime criadoEm) {

    public static SnapshotResponse de(PersonagemSnapshot s) {
        return new SnapshotResponse(s.getId(), s.getPersonagemId(), s.getMotivo(), s.getCriadoEm());
    }
}
