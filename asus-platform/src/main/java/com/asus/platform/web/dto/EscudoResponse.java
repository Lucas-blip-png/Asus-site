package com.asus.platform.web.dto;

import java.util.List;

/** Visao consolidada do Escudo do Mestre (Fase 8). */
public record EscudoResponse(
        CampanhaResponse campanha,
        List<PersonagemResponse> personagens,
        List<CampanhaMembroResponse> membros,
        List<RolagemResponse> rolagens) {}
