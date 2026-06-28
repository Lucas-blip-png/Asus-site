package com.asus.platform.web.dto;

import com.asus.platform.domain.Habilidade;
import com.asus.platform.domain.HabilidadePersonagem;

/** Habilidade escolhida por um personagem: dados do catalogo com overrides aplicados. */
public record HabilidadeEscolhidaResponse(
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
        String efeito) {

    public static HabilidadeEscolhidaResponse de(Habilidade h, HabilidadePersonagem hp) {
        return new HabilidadeEscolhidaResponse(
                h.getCodigo(),
                hp.getNomeCustom() != null ? hp.getNomeCustom() : h.getNome(),
                h.getClasseCodigo(),
                hp.getTipoCustom() != null ? hp.getTipoCustom() : h.getTipo(),
                hp.getCustoCustom() != null ? hp.getCustoCustom() : h.getCusto(),
                hp.getCustoTipoCustom() != null ? hp.getCustoTipoCustom() : h.getCustoTipo(),
                h.getRequisito(),
                h.getNivelMinimo(),
                h.getAtributoRequisito(),
                h.getValorAtributoRequisito(),
                hp.getEfeitoCustom() != null ? hp.getEfeitoCustom() : h.getEfeito());
    }
}
