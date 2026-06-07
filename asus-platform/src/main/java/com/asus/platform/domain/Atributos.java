package com.asus.platform.domain;

import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Valor dos sete atributos ASUS. Usado como base e como final (apos bonus). */
@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Atributos {

    private int forca;
    private int constituicao;
    private int destreza;
    private int agilidade;
    private int inteligencia;
    private int sabedoria;
    private int carisma;

    /** Acessa o valor de um atributo pelo enum. */
    public int get(Atributo atributo) {
        return switch (atributo) {
            case FORCA -> forca;
            case CONSTITUICAO -> constituicao;
            case DESTREZA -> destreza;
            case AGILIDADE -> agilidade;
            case INTELIGENCIA -> inteligencia;
            case SABEDORIA -> sabedoria;
            case CARISMA -> carisma;
        };
    }

    /** Retorna uma copia com o valor de um atributo somado a um delta. */
    public Atributos comBonus(Atributo atributo, int delta) {
        Atributos novo = copia();
        switch (atributo) {
            case FORCA -> novo.forca += delta;
            case CONSTITUICAO -> novo.constituicao += delta;
            case DESTREZA -> novo.destreza += delta;
            case AGILIDADE -> novo.agilidade += delta;
            case INTELIGENCIA -> novo.inteligencia += delta;
            case SABEDORIA -> novo.sabedoria += delta;
            case CARISMA -> novo.carisma += delta;
        }
        return novo;
    }

    public Atributos copia() {
        return Atributos.builder()
                .forca(forca).constituicao(constituicao).destreza(destreza)
                .agilidade(agilidade).inteligencia(inteligencia)
                .sabedoria(sabedoria).carisma(carisma)
                .build();
    }
}
