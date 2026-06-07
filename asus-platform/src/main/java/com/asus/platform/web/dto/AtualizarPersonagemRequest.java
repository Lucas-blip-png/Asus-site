package com.asus.platform.web.dto;

import jakarta.validation.Valid;
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
        String divindade,
        Integer nivel,
        Integer xpAtual,
        Long avatarAssetId,
        @Valid AtributosDto atributosBase,
        Map<String, Integer> pericias,
        String anotacoes,
        String aparencia,
        String personalidade,
        String historico,
        String objetivo) {}
