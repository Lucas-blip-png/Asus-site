package com.asus.platform.web.dto;

import com.asus.platform.domain.Habilidade;

/**
 * Habilidade que o personagem pode ver na lista para escolher.
 * {@code bloqueada=true} quando o personagem ainda nao atende ao requisito
 * (nivel ou atributo); nesse caso {@code motivo} explica o porque e o front
 * mostra a habilidade, mas nao deixa pegar ate atingir o requisito.
 */
public record HabilidadeDisponivelResponse(
        String codigo,
        String nome,
        String classeCodigo,
        String tipo,
        int custo,
        String custoTipo,
        String requisito,
        int nivelMinimo,
        String atributoRequisito,
        int valorAtributoRequisito,
        String efeito,
        boolean bloqueada,
        String motivo) {

    public static HabilidadeDisponivelResponse de(Habilidade h, boolean bloqueada, String motivo) {
        return new HabilidadeDisponivelResponse(
                h.getCodigo(), h.getNome(), h.getClasseCodigo(), h.getTipo(),
                h.getCusto(), h.getCustoTipo(), h.getRequisito(), h.getNivelMinimo(),
                h.getAtributoRequisito(), h.getValorAtributoRequisito(), h.getEfeito(),
                bloqueada, motivo);
    }
}
