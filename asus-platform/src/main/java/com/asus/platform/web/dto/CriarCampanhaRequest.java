package com.asus.platform.web.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

public record CriarCampanhaRequest(
        @NotBlank String nome,
        String descricao,
        /** Usuario mestre. Se nulo, usa o dono da organizacao. */
        Long mestreId,
        @Valid CampanhaConfigDto config) {}
