package com.asus.platform.domain;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * Mapa papel-da-campanha -> permissoes (plano, secao 9).
 *
 * <p>Estrutura pronta para o controle de acesso (criterio de aceite 10). A
 * aplicacao efetiva por usuario so se torna obrigatoria na Fase 7 (login real),
 * quando o usuario autenticado vem do JWT. Por enquanto serve para guardar
 * acoes sensiveis quando o {@code usuarioId} e informado.</p>
 */
public final class Permissoes {

    private static final Map<PapelCampanha, Set<Permissao>> MAPA = new EnumMap<>(PapelCampanha.class);

    static {
        MAPA.put(PapelCampanha.MESTRE, EnumSet.allOf(Permissao.class));
        MAPA.put(PapelCampanha.CO_MESTRE, EnumSet.of(
                Permissao.VER_FICHA, Permissao.EDITAR_FICHA, Permissao.EDITAR_STATUS,
                Permissao.EDITAR_INVENTARIO, Permissao.EDITAR_MAGIAS,
                Permissao.VER_ROLAGENS_OCULTAS, Permissao.GERENCIAR_CAMPANHA,
                Permissao.CONVIDAR_JOGADORES, Permissao.GERENCIAR_ASSETS));
        MAPA.put(PapelCampanha.JOGADOR, EnumSet.of(
                Permissao.VER_FICHA, Permissao.EDITAR_STATUS,
                Permissao.EDITAR_INVENTARIO, Permissao.EDITAR_MAGIAS));
        MAPA.put(PapelCampanha.OBSERVADOR, EnumSet.of(Permissao.VER_FICHA));
    }

    private Permissoes() {
    }

    public static Set<Permissao> de(PapelCampanha papel) {
        return MAPA.getOrDefault(papel, EnumSet.noneOf(Permissao.class));
    }

    public static boolean pode(PapelCampanha papel, Permissao permissao) {
        return papel != null && de(papel).contains(permissao);
    }
}
