package com.asus.platform.web.dto;

import com.asus.platform.domain.Campanha;
import java.time.LocalDateTime;

public record CampanhaResponse(
        Long id,
        Long organizacaoId,
        Long mestreId,
        String rulesetSystemId,
        String nome,
        String descricao,
        String capaAssetId,
        String discordWebhookUrl,
        CampanhaConfigDto config,
        boolean arquivada,
        LocalDateTime criadoEm,
        LocalDateTime atualizadoEm) {

    public static CampanhaResponse de(Campanha c, String systemId) {
        return new CampanhaResponse(
                c.getId(), c.getOrganizacaoId(), c.getMestreId(), systemId,
                c.getNome(), c.getDescricao(), c.getCapaAssetId(), c.getDiscordWebhookUrl(),
                CampanhaConfigDto.de(c.getConfig()), c.isArquivada(),
                c.getCriadoEm(), c.getAtualizadoEm());
    }
}
