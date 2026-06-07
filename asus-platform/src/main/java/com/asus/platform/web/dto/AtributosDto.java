package com.asus.platform.web.dto;

import com.asus.platform.domain.Atributos;

public record AtributosDto(int forca, int agilidade, int vigor, int intelecto, int presenca) {

    public static AtributosDto de(Atributos a) {
        if (a == null) {
            return new AtributosDto(0, 0, 0, 0, 0);
        }
        return new AtributosDto(a.getForca(), a.getAgilidade(), a.getVigor(), a.getIntelecto(), a.getPresenca());
    }

    public Atributos paraEntidade() {
        return Atributos.builder()
                .forca(forca).agilidade(agilidade).vigor(vigor)
                .intelecto(intelecto).presenca(presenca)
                .build();
    }
}
