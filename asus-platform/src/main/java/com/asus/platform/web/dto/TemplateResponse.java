package com.asus.platform.web.dto;

import com.asus.platform.domain.Template;
import java.time.LocalDateTime;

public record TemplateResponse(
        Long id,
        Long organizacaoId,
        Long gameSystemId,
        Long autorUsuarioId,
        String tipo,
        String nome,
        String descricao,
        String jsonConteudo,
        boolean oficial,
        boolean publico,
        LocalDateTime criadoEm) {

    public static TemplateResponse de(Template t) {
        return new TemplateResponse(t.getId(), t.getOrganizacaoId(), t.getGameSystemId(),
                t.getAutorUsuarioId(), t.getTipo(), t.getNome(), t.getDescricao(),
                t.getJsonConteudo(), t.isOficial(), t.isPublico(), t.getCriadoEm());
    }
}
