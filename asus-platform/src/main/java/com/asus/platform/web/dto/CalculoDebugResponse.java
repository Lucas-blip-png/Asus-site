package com.asus.platform.web.dto;

import java.util.List;

/** Debug do calculo da ficha (criterio de aceite 9). */
public record CalculoDebugResponse(
        AtributosDto atributosBase,
        AtributosDto atributosFinais,
        StatusDto status,
        List<PericiaCalculadaDto> pericias,
        List<String> passos) {}
