package com.asus.platform.web.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CriarPersonagemRequest(
        @NotBlank String nome,
        String jogador,
        @NotBlank String racaCodigo,
        @NotBlank String classeCodigo,
        Integer nivel,
        @NotNull @Valid AtributosDto atributosBase) {

    public int nivelOuPadrao() {
        return (nivel == null || nivel < 1) ? 1 : nivel;
    }
}
