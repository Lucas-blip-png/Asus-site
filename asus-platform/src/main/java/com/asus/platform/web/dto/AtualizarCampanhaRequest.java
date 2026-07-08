package com.asus.platform.web.dto;

import jakarta.validation.Valid;

/** Atualizacao parcial da campanha. Campos nulos sao ignorados. */
public record AtualizarCampanhaRequest(
        String nome,
        String descricao,
        String capaAssetId,
        String discordWebhookUrl,
        Boolean arquivada,
        @Valid CampanhaConfigDto config) {}
