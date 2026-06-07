package com.asus.platform.web.dto;

/** Formato de exportacao (plano, secao 12.3 / criterio de aceite 6). */
public record ExportPersonagemResponse(
        String exportVersion,
        String system,
        String rulesetVersion,
        PersonagemResponse personagem) {

    public static ExportPersonagemResponse de(PersonagemResponse p) {
        return new ExportPersonagemResponse("1.0", p.system(), p.rulesetVersion(), p);
    }
}
