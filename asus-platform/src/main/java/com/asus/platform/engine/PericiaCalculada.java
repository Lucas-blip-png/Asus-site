package com.asus.platform.engine;

/** Resultado do calculo de uma pericia. */
public record PericiaCalculada(
        String codigo,
        String nome,
        String atributoBase,
        int valor
) {}
