package com.asus.platform.web.dto;

/** Atualizacao parcial de status. Campos nulos sao ignorados. */
public record AtualizarStatusRequest(
        Integer pvAtual,
        Integer pmAtual,
        Integer peAtual) {}
