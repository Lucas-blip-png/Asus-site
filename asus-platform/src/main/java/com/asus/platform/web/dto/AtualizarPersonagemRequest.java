package com.asus.platform.web.dto;

import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;

/**
 * Atualizacao de personagem (plano, secao 21.3 — PUT). Campos nulos sao ignorados.
 * Alterar nivel/raca/classe/atributos/trilha/pericias dispara recalculo + snapshot.
 */
public record AtualizarPersonagemRequest(
        String nome,
        String jogador,
        String racaCodigo,
        String classeCodigo,
        String trilhaCodigo,
        String classeSecundariaCodigo,
        String trilhaSecundariaCodigo,
        String divindade,
        Integer nivel,
        Integer xpAtual,
        Long avatarAssetId,
        @Valid AtributosDto atributosBase,
        Map<String, Integer> pericias,
        List<PericiaCustomDto> periciasCustom,
        String anotacoes,
        String aparencia,
        String personalidade,
        String historico,
        String objetivo) {}
