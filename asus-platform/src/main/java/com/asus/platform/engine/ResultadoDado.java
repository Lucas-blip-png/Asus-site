package com.asus.platform.engine;

import java.util.List;

/** Resultado bruto de uma rolagem: faces sorteadas, modificador e total. */
public record ResultadoDado(List<Integer> dados, int modificador, int total) {}
