package com.asus.platform.web.dto;

/** Atualizacao parcial de status. Campos nulos sao ignorados.
 *  pvMax/pmMax/peMax definem um teto manual (override) que persiste entre recalculos. */
public record AtualizarStatusRequest(
        Integer pvAtual,
        Integer pmAtual,
        Integer peAtual,
        Integer pvMax,
        Integer pmMax,
        Integer peMax) {}
