package com.asus.platform.web.dto;

import com.asus.platform.domain.Status;

public record StatusDto(
        int pvMax, int pvAtual,
        int pmMax, int pmAtual,
        int peMax, int peAtual,
        int defesa) {

    public static StatusDto de(Status s) {
        if (s == null) {
            return new StatusDto(0, 0, 0, 0, 0, 0, 0);
        }
        return new StatusDto(s.getPvMax(), s.getPvAtual(), s.getPmMax(),
                s.getPmAtual(), s.getPeMax(), s.getPeAtual(), s.getDefesa());
    }
}
