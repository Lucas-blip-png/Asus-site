package com.asus.platform.engine;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Expressao de dados no formato {@code NdF+M} (ex: {@code 1d20+5}, {@code 2d6}, {@code d8-1}),
 * com suporte a "keep" para vantagem/desvantagem: {@code 2d20kh1+3} (mantem o maior)
 * e {@code 2d20kl1} (mantem o menor).
 *
 * <p>Parsing puro e separado da rolagem: assim o parser e testavel sem Spring e a
 * rolagem usa um {@link Dado} injetavel (deterministico nos testes).</p>
 */
public record ExpressaoDado(int quantidade, int faces, int modificador, char keep, int keepQtd) {

    private static final Pattern PADRAO = Pattern.compile(
            "\\s*(\\d*)\\s*[dD]\\s*(\\d+)\\s*(?:[kK]([hHlL])(\\d*))?\\s*([+-]\\s*\\d+)?\\s*");

    /** Expressao simples, sem keep. */
    public ExpressaoDado(int quantidade, int faces, int modificador) {
        this(quantidade, faces, modificador, '\0', 0);
    }

    public static ExpressaoDado parse(String expressao) {
        if (expressao == null || expressao.isBlank()) {
            throw new IllegalArgumentException("Expressao de rolagem vazia");
        }
        Matcher m = PADRAO.matcher(expressao);
        if (!m.matches()) {
            throw new IllegalArgumentException(
                    "Expressao invalida: '" + expressao + "' (use NdF+M, ex: 1d20+5, ou 2d20kh1 p/ vantagem)");
        }
        int quantidade = m.group(1).isEmpty() ? 1 : Integer.parseInt(m.group(1));
        int faces = Integer.parseInt(m.group(2));
        char keep = m.group(3) == null ? '\0' : Character.toLowerCase(m.group(3).charAt(0));
        int keepQtd = 0;
        if (keep != '\0') {
            keepQtd = (m.group(4) == null || m.group(4).isEmpty()) ? 1 : Integer.parseInt(m.group(4));
        }
        int modificador = m.group(5) == null ? 0
                : Integer.parseInt(m.group(5).replaceAll("\\s", ""));

        if (quantidade < 1 || quantidade > 100) {
            throw new IllegalArgumentException("Quantidade de dados deve estar entre 1 e 100");
        }
        if (faces < 2 || faces > 1000) {
            throw new IllegalArgumentException("Faces do dado devem estar entre 2 e 1000");
        }
        if (keep != '\0' && (keepQtd < 1 || keepQtd > quantidade)) {
            throw new IllegalArgumentException("Keep deve manter entre 1 e " + quantidade + " dados");
        }
        return new ExpressaoDado(quantidade, faces, modificador, keep, keepQtd);
    }

    /** Rola usando a fonte de aleatoriedade informada. */
    public ResultadoDado rolar(Dado dado) {
        List<Integer> sorteados = new ArrayList<>(quantidade);
        for (int i = 0; i < quantidade; i++) {
            sorteados.add(dado.rolar(faces));
        }
        int soma = somaMantidos(sorteados);
        return new ResultadoDado(List.copyOf(sorteados), modificador, soma + modificador);
    }

    /** Soma apenas os dados mantidos (todos quando nao ha keep). */
    private int somaMantidos(List<Integer> dados) {
        if (keep == '\0') {
            return dados.stream().mapToInt(Integer::intValue).sum();
        }
        return dados.stream()
                .sorted(keep == 'h' ? Comparator.reverseOrder() : Comparator.naturalOrder())
                .limit(keepQtd)
                .mapToInt(Integer::intValue)
                .sum();
    }

    /** Dado "efetivo" (o mantido) quando ha um unico dado relevante — para critico/falha. */
    public Integer dadoEfetivo(List<Integer> dados) {
        if (keep == '\0') {
            return dados.size() == 1 ? dados.get(0) : null;
        }
        if (keepQtd != 1) {
            return null;
        }
        return keep == 'h'
                ? dados.stream().max(Integer::compareTo).orElse(null)
                : dados.stream().min(Integer::compareTo).orElse(null);
    }

    /** Forma canonica da expressao, ex: {@code 1d20+5} ou {@code 2d20kh1+3}. */
    public String canonico() {
        String base = quantidade + "d" + faces;
        if (keep != '\0') {
            base += "k" + keep + keepQtd;
        }
        if (modificador > 0) {
            return base + "+" + modificador;
        }
        if (modificador < 0) {
            return base + modificador;
        }
        return base;
    }
}
