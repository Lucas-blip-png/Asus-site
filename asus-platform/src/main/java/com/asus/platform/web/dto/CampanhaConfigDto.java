package com.asus.platform.web.dto;

import com.asus.platform.domain.CampanhaConfig;

/** Espelho da {@link CampanhaConfig} (plano, secao 8.2). */
public record CampanhaConfigDto(
        boolean usarBencoes,
        boolean usarDivindades,
        boolean usarMagiaAvancada,
        boolean usarCombateTatico,
        boolean permitirHomebrew,
        boolean rolagemOcultaPermitida) {

    public static CampanhaConfigDto de(CampanhaConfig c) {
        if (c == null) {
            return new CampanhaConfigDto(false, false, false, false, false, true);
        }
        return new CampanhaConfigDto(c.isUsarBencoes(), c.isUsarDivindades(),
                c.isUsarMagiaAvancada(), c.isUsarCombateTatico(),
                c.isPermitirHomebrew(), c.isRolagemOcultaPermitida());
    }

    public CampanhaConfig paraEntidade() {
        return CampanhaConfig.builder()
                .usarBencoes(usarBencoes)
                .usarDivindades(usarDivindades)
                .usarMagiaAvancada(usarMagiaAvancada)
                .usarCombateTatico(usarCombateTatico)
                .permitirHomebrew(permitirHomebrew)
                .rolagemOcultaPermitida(rolagemOcultaPermitida)
                .build();
    }
}
