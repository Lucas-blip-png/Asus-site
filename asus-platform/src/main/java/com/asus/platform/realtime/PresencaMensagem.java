package com.asus.platform.realtime;

/** Aviso de presenca enviado pelos clientes (entrou/saiu/online). */
public record PresencaMensagem(Long usuarioId, String nome, String status) {}
