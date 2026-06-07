package com.asus.platform.web.dto;

import com.asus.platform.domain.Atributo;

public record AtributoInfo(String codigo, String nome, String sigla, String descricao) {

    public static AtributoInfo de(Atributo a, String nome, String descricao) {
        return new AtributoInfo(a.name(), nome, a.getSigla(), descricao);
    }
}
