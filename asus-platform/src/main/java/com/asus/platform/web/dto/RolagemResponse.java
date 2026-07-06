package com.asus.platform.web.dto;

import com.asus.platform.domain.Rolagem;
import java.time.LocalDateTime;

/**
 * Resultado de uma rolagem. Para rolagens ocultas ainda nao reveladas, os campos
 * de resultado (total, detalhe, naturalD20, critico, falhaCritica) vem nulos.
 */
public record RolagemResponse(
        Long id,
        Long campanhaId,
        Long personagemId,
        Long usuarioId,
        String expressao,
        String rotulo,
        Integer total,
        String detalhe,
        Integer naturalD20,
        Boolean critico,
        Boolean falhaCritica,
        boolean oculta,
        boolean revelada,
        LocalDateTime criadoEm,
        String personagemNome) {

    public static RolagemResponse de(Rolagem r, boolean revelarConteudo) {
        return de(r, revelarConteudo, null);
    }

    public static RolagemResponse de(Rolagem r, boolean revelarConteudo, String personagemNome) {
        boolean mostrar = revelarConteudo || !r.isOculta() || r.isRevelada();
        return new RolagemResponse(
                r.getId(), r.getCampanhaId(), r.getPersonagemId(), r.getUsuarioId(),
                r.getExpressao(), r.getRotulo(),
                mostrar ? r.getTotal() : null,
                mostrar ? r.getDetalhe() : null,
                mostrar ? r.getNaturalD20() : null,
                mostrar ? r.isCritico() : null,
                mostrar ? r.isFalhaCritica() : null,
                r.isOculta(), r.isRevelada(), r.getCriadoEm(), personagemNome);
    }
}
