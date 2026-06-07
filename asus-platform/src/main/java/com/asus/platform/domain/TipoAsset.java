package com.asus.platform.domain;

import java.util.Locale;

/** Tipos de asset (plano, Seção 13.2). */
public enum TipoAsset {
    AVATAR_PERSONAGEM,
    CAPA_CAMPANHA,
    MAPA,
    HANDOUT,
    TOKEN,
    PDF,
    OVERLAY,
    OUTRO;

    /** Converte um texto livre em TipoAsset; desconhecido vira {@link #OUTRO}. */
    public static TipoAsset deOuOutro(String texto) {
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
