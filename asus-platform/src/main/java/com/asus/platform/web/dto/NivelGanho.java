package com.asus.platform.web.dto;

/** O que o personagem ganhou ao alcancar um nivel (conteudo do popup de level-up). */
public record NivelGanho(int nivel, String recompensa, int limiteAtributo) {}
