package com.asus.platform.web.dto;

/** Atualiza XP e/ou nivel; o servico sobe de nivel automaticamente pelo XP. */
public record ProgressoRequest(Integer xpAtual, Integer nivel) {}
