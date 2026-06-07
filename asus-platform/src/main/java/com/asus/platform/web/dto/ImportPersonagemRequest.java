package com.asus.platform.web.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Importacao de personagem (plano, secao 12 / criterio de aceite 6).
 *
 * <p>Aceita o mesmo envelope produzido por {@link ExportPersonagemResponse}:
 * campos extras do export (atributos finais, status, pericias...) sao ignorados,
 * pois a ficha e recalculada na importacao.</p>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ImportPersonagemRequest(
        String exportVersion,
        String system,
        String rulesetVersion,
        @NotNull @Valid PersonagemImport personagem) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record PersonagemImport(
            @NotNull Long organizacaoId,
            @NotBlank String nome,
            String jogador,
            @NotBlank String racaCodigo,
            @NotBlank String classeCodigo,
            Integer nivel,
            Integer xpAtual,
            @NotNull @Valid AtributosDto atributosBase) {

        public int nivelOuPadrao() {
            return (nivel == null || nivel < 1) ? 1 : nivel;
        }

        public int xpOuZero() {
            return xpAtual == null ? 0 : xpAtual;
        }
    }
}
