package com.asus.platform.engine;

/**
 * Fonte de aleatoriedade dos dados, isolada atras de uma interface para que os
 * testes possam injetar resultados deterministicos (criar criticos/falhas a vontade).
 */
public interface Dado {

    /** Sorteia uma face entre 1 e {@code faces} (inclusive). */
    int rolar(int faces);
}
