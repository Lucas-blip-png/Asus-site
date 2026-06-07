package com.asus.platform.web.dto;

import com.asus.platform.domain.PapelCampanha;

/**
 * Criacao de convite. Todos os campos sao opcionais:
 * papel padrao JOGADOR, sem limite de usos e sem expiracao.
 * {@code usuarioId} (quem cria) e usado para checar permissao quando informado.
 */
public record CriarConviteRequest(
        PapelCampanha papel,
        Integer maxUsos,
        Integer expiraEmDias,
        Long usuarioId) {}
