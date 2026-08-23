package com.asus.platform.web.dto;

import com.asus.platform.domain.Auditoria;
import java.time.LocalDateTime;

/** Linha do historico de auditoria (plano, secao 10). */
public record AuditoriaResponse(
        Long id,
        Long organizacaoId,
        Long usuarioId,
        /** Nome de quem fez a alteracao (para o historico da ficha); pode ser nulo. */
        String usuarioNome,
        String acao,
        String entidade,
        Long entidadeId,
        String campo,
        String valorAnterior,
        String valorNovo,
        LocalDateTime criadoEm) {

    public static AuditoriaResponse de(Auditoria a) {
        return de(a, null);
    }

    public static AuditoriaResponse de(Auditoria a, String usuarioNome) {
        return new AuditoriaResponse(a.getId(), a.getOrganizacaoId(), a.getUsuarioId(), usuarioNome,
                a.getAcao(), a.getEntidade(), a.getEntidadeId(), a.getCampo(),
                a.getValorAnterior(), a.getValorNovo(), a.getCriadoEm());
    }
}
