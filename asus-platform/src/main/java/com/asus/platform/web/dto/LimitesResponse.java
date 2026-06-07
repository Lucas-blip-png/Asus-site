package com.asus.platform.web.dto;

import com.asus.platform.domain.LimitesPlano;

public record LimitesResponse(
        int maxPersonagens,
        int maxCampanhas,
        int maxJogadoresPorCampanha,
        boolean overlayObs,
        int historicoRolagensDias,
        long assetsBytesMax) {

    public static LimitesResponse de(LimitesPlano l) {
        return new LimitesResponse(l.maxPersonagens(), l.maxCampanhas(),
                l.maxJogadoresPorCampanha(), l.overlayObs(),
                l.historicoRolagensDias(), l.assetsBytesMax());
    }
}
