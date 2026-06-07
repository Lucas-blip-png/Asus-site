package com.asus.platform.util;

/**
 * Sanitizacao basica de HTML em descricoes (plano, Seção 20.1).
 *
 * <p>Remove tags HTML para evitar XSS armazenado. Em producao, prefira uma
 * biblioteca dedicada (OWASP Java HTML Sanitizer / jsoup) com allow-list.</p>
 */
public final class Sanitizador {

    private Sanitizador() {
    }

    public static String limpar(String texto) {
        if (texto == null) {
            return null;
        }
        return texto.replaceAll("<[^>]*>", "").trim();
    }
}
