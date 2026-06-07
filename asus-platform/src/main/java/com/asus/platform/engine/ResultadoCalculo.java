package com.asus.platform.engine;

import com.asus.platform.domain.Atributos;
import com.asus.platform.domain.Status;
import java.util.List;

/**
 * Saida do engine: atributos finais, status derivado, pericias e os
 * "passos" do calculo (para o endpoint de debug — criterio de aceite 9).
 */
public record ResultadoCalculo(
        Atributos atributosFinais,
        Status status,
        List<PericiaCalculada> pericias,
        List<String> passos
) {}
