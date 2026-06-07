package com.asus.platform.engine;

/**
 * Resultado do calculo de uma pericia (ASUS): valor treinado e o teto.
 * Teste = 1d20 + treino. A pericia pode subir ate 2x o atributo governante (cap).
 */
public record PericiaCalculada(
        String codigo,
        String nome,
        String atributoBase,
        String sigla,
        int treino,
        int cap
) {}
