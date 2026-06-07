package com.asus.platform.domain;

import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Status derivado da ficha (PV/PM/PE + defesa). */
@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Status {

    private int pvMax;
    private int pvAtual;

    private int pmMax;
    private int pmAtual;

    private int peMax;
    private int peAtual;

    private int defesa;
}
