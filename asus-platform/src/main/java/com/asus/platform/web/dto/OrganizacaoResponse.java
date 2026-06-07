package com.asus.platform.web.dto;

import com.asus.platform.domain.Organizacao;
import java.time.LocalDateTime;

public record OrganizacaoResponse(
        Long id, String nome, String slug, String plano, LocalDateTime criadoEm) {

    public static OrganizacaoResponse de(Organizacao o) {
        return new OrganizacaoResponse(o.getId(), o.getNome(), o.getSlug(),
                o.getPlano() == null ? null : o.getPlano().name(), o.getCriadoEm());
    }
}
