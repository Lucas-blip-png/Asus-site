package com.asus.platform.domain;

import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Valor dos cinco atributos ASUS. Usado como base e como final (apos bonus). */
@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Atributos {

    private int forca;
    private int agilidade;
    private int vigor;
    private int intelecto;
    private int presenca;

    /** Acessa o valor de um atributo pelo enum. */
    public int get(Atributo atributo) {
        return switch (atributo) {
            case FORCA -> forca;
            case AGILIDADE -> agilidade;
            case VIGOR -> vigor;
            case INTELECTO -> intelecto;
            case PRESENCA -> presenca;
        };
    }

    /** Retorna uma copia com o valor de um atributo somado a um delta. */
    public Atributos comBonus(Atributo atributo, int delta) {
        Atributos novo = Atributos.builder()
                .forca(forca)
                .agilidade(agilidade)
                .vigor(vigor)
                .intelecto(intelecto)
                .presenca(presenca)
                .build();
        switch (atributo) {
            case FORCA -> novo.forca += delta;
            case AGILIDADE -> novo.agilidade += delta;
            case VIGOR -> novo.vigor += delta;
            case INTELECTO -> novo.intelecto += delta;
            case PRESENCA -> novo.presenca += delta;
        }
        return novo;
    }
}
