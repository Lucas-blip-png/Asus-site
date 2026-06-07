package com.asus.platform.web.dto;

import java.util.List;

/** Resposta da atualizacao de progresso: a ficha + os niveis ganhos (para o popup). */
public record ProgressoResponse(PersonagemResponse personagem, List<NivelGanho> niveisGanhos) {}
