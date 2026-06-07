package com.asus.platform.web.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.Map;

public record CriarPersonagemRequest(
        @NotBlank String nome,
        String jogador,
        @NotBlank String racaCodigo,
        @NotBlank String classeCodigo,
        String trilhaCodigo,
        String divindade,
        Integer nivel,
        @NotNull @Valid AtributosDto atributosBase,
        /** Treino de pericias por codigo, ex: {"COMBATE":3,"VIGOR":2}. Opcional. */
        Map<String, Integer> pericias) {

    public int nivelOuPadrao() {
        return (nivel == null || nivel < 1) ? 1 : nivel;
    }
}
