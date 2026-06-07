package com.asus.platform.web.dto;

import com.asus.platform.domain.Asset;
import java.time.LocalDateTime;

public record AssetResponse(
        Long id,
        Long organizacaoId,
        Long usuarioId,
        String tipo,
        String nomeOriginal,
        String mimeType,
        long tamanhoBytes,
        boolean publico,
        String url,
        LocalDateTime criadoEm) {

    public static AssetResponse de(Asset a) {
        return new AssetResponse(a.getId(), a.getOrganizacaoId(), a.getUsuarioId(),
                a.getTipo(), a.getNomeOriginal(), a.getMimeType(), a.getTamanhoBytes(),
                a.isPublico(), "/api/assets/" + a.getId() + "/conteudo", a.getCriadoEm());
    }
}
