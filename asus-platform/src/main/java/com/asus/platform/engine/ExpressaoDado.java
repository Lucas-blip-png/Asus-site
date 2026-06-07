package com.asus.platform.engine;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Expressao de dados no formato {@code NdF+M} (ex: {@code 1d20+5}, {@code 2d6}, {@code d8-1}).
 *
 * <p>Parsing puro e separado da rolagem: assim o parser e testavel sem Spring e a
 * rolagem usa um {@link Dado} injetavel (deterministico nos testes).</p>
 */
public record ExpressaoDado(int quantidade, int faces, int modificador) {

    private static final Pattern PADRAO =
            Pattern.compile("\\s*(\\d*)\\s*[dD]\\s*(\\d+)\\s*([+-]\\s*\\d+)?\\s*");

    public static ExpressaoDado parse(String expressao) {
        if (expressao == null || expressao.isBlank()) {
            throw new IllegalArgumentException("Expressao de rolagem vazia");
        }
        Matcher m = PADRAO.matcher(expressao);
        if (!m.matches()) {
            throw new IllegalArgumentException(
                    "Expressao invalida: '" + expressao + "' (use NdF+M, ex: 1d20+5)");
        }
        int quantidade = m.group(1).isEmpty() ? 1 : Integer.parseInt(m.group(1));
        int faces = Integer.parseInt(m.group(2));
        int modificador = m.group(3) == null ? 0
                : Integer.parseInt(m.group(3).replaceAll("\\s", ""));

        if (quantidade < 1 || quantidade > 100) {
            throw new IllegalArgumentException("Quantidade de dados deve estar entre 1 e 100");
        }
        if (faces < 2 || faces > 1000) {
            throw new IllegalArgumentException("Faces do dado devem estar entre 2 e 1000");
        }
        return new ExpressaoDado(quantidade, faces, modificador);
    }

    /** Rola usando a fonte de aleatoriedade informada. */
    public ResultadoDado rolar(Dado dado) {
        List<Integer> sorteados = new ArrayList<>(quantidade);
        int soma = 0;
        for (int i = 0; i < quantidade; i++) {
            int face = dado.rolar(faces);
            sorteados.add(face);
            soma += face;
        }
        return new ResultadoDado(List.copyOf(sorteados), modificador, soma + modificador);
    }

    /** Forma canonica da expressao, ex: {@code 1d20+5}. */
    public String canonico() {
        String base = quantidade + "d" + faces;
        if (modificador > 0) {
            return base + "+" + modificador;
        }
        if (modificador < 0) {
            return base + modificador;
        }
        return base;
    }
}
