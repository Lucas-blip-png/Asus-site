package com.asus.platform.engine;

/**
 * Resultado do calculo de uma pericia (ASUS): treino distribuido pelo jogador,
 * o teto (2x atributo governante) e o bonus fixo vindo de classe/trilha (nao editavel).
 * Teste = 1d20 + treino + bonus.
 */
public record PericiaCalculada(
        String codigo,
        String nome,
        String atributoBase,
        String sigla,
        int treino,
        int cap,
        int bonus
) {}
