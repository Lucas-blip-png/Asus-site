package com.asus.platform.web.dto;

import jakarta.validation.Valid;

/**
 * Atualizacao de personagem (plano, secao 21.3 — PUT). Campos nulos sao ignorados.
 * Alterar nivel/raca/classe/atributos dispara recalculo da ficha e um snapshot.
 */
public record AtualizarPersonagemRequest(
        String nome,
        String jogador,
        String racaCodigo,
        String classeCodigo,
        Integer nivel,
        Integer xpAtual,
        @Valid AtributosDto atributosBase,
        String anotacoes,
        String aparencia,
        String personalidade,
        String historico,
        String objetivo) {}
