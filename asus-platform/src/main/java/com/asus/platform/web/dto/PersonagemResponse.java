package com.asus.platform.web.dto;

import java.time.LocalDateTime;
import java.util.List;

/** Ficha completa do personagem (criterio de aceite 5). */
public record PersonagemResponse(
        Long id,
        Long organizacaoId,
        String nome,
        String jogador,
        String system,
        String rulesetVersion,
        String racaCodigo,
        String racaNome,
        String classeCodigo,
        String classeNome,
        String trilhaCodigo,
        String trilhaNome,
        String divindade,
        int nivel,
        int xpAtual,
        AtributosDto atributosBase,
        AtributosDto atributosFinais,
        StatusDto status,
        int deslocamento,
        int cargaMaxima,
        int limiteHabilidades,
        int limiteFeiticos,
        int limiteBencaos,
        List<PericiaCalculadaDto> pericias,
        boolean arquivado,
        LocalDateTime criadoEm,
        LocalDateTime atualizadoEm) {}
