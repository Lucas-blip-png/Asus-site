package com.asus.platform.web.dto;

import com.asus.platform.domain.Atributos;

/** Os sete atributos do ASUS. */
public record AtributosDto(
        int forca,
        int constituicao,
        int destreza,
        int agilidade,
        int inteligencia,
        int sabedoria,
        int carisma) {

    public static AtributosDto de(Atributos a) {
        if (a == null) {
            return new AtributosDto(0, 0, 0, 0, 0, 0, 0);
        }
        return new AtributosDto(a.getForca(), a.getConstituicao(), a.getDestreza(),
                a.getAgilidade(), a.getInteligencia(), a.getSabedoria(), a.getCarisma());
    }

    public Atributos paraEntidade() {
        return Atributos.builder()
                .forca(forca).constituicao(constituicao).destreza(destreza)
                .agilidade(agilidade).inteligencia(inteligencia)
                .sabedoria(sabedoria).carisma(carisma)
                .build();
    }
}
