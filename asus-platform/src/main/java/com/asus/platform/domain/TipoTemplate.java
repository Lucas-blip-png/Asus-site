package com.asus.platform.domain;

import java.util.Locale;

/** Tipos de template (plano, Seção 15.1). */
public enum TipoTemplate {
    ATAQUE,
    MAGIA,
    HABILIDADE,
    ITEM,
    NPC,
    MONSTRO,
    CAMPANHA,
    OVERLAY,
    OUTRO;

    public static TipoTemplate deOuOutro(String texto) {
        if (texto == null || texto.isBlank()) {
            return OUTRO;
        }
        try {
            return valueOf(texto.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return OUTRO;
        }
    }
}
