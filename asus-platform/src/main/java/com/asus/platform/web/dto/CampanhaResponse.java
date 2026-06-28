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
        String anotacoes,
        String capaAssetId,
        CampanhaConfigDto config,
        boolean arquivada,
        LocalDateTime criadoEm,
        LocalDateTime atualizadoEm) {

    public static CampanhaResponse de(Campanha c, String systemId) {
        return new CampanhaResponse(
                c.getId(), c.getOrganizacaoId(), c.getMestreId(), systemId,
                c.getNome(), c.getDescricao(), c.getAnotacoes(), c.getCapaAssetId(),
                CampanhaConfigDto.de(c.getConfig()), c.isArquivada(),
                c.getCriadoEm(), c.getAtualizadoEm());
    }
}
