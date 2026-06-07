package com.asus.platform.domain;

import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Configuracao da campanha (plano, secao 8.2). */
@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CampanhaConfig {

    private boolean usarBencoes;
    private boolean usarDivindades;
    private boolean usarMagiaAvancada;
    private boolean usarCombateTatico;
    private boolean permitirHomebrew;
    private boolean rolagemOcultaPermitida;
}
