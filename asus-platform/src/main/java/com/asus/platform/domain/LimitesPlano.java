package com.asus.platform.domain;

/**
 * Limites por plano (plano de implementacao, Seção 4.2).
 *
 * <p>{@link #ILIMITADO} representa "sem limite". Valores de armazenamento em bytes.</p>
 */
public record LimitesPlano(
        int maxPersonagens,
        int maxCampanhas,
        int maxJogadoresPorCampanha,
        boolean overlayObs,
        int historicoRolagensDias,
        long assetsBytesMax) {

    public static final int ILIMITADO = Integer.MAX_VALUE;
    private static final long MB = 1024L * 1024L;

    public static LimitesPlano de(Plano plano) {
        Plano p = plano == null ? Plano.FREE : plano;
        return switch (p) {
            case FREE   -> new LimitesPlano(5, 1, 5, false, 7, 100 * MB);
            case PRO    -> new LimitesPlano(ILIMITADO, 3, 8, true, 30, 1024 * MB);
            case MESTRE -> new LimitesPlano(ILIMITADO, ILIMITADO, 12, true, ILIMITADO, 5L * 1024 * MB);
            case GUILD  -> new LimitesPlano(ILIMITADO, ILIMITADO, 50, true, ILIMITADO, 25L * 1024 * MB);
        };
    }
}
