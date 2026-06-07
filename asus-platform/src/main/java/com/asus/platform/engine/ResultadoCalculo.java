package com.asus.platform.engine;

import com.asus.platform.domain.Atributos;
import com.asus.platform.domain.Status;
import java.util.List;

/**
 * Saida do engine: atributos finais, status derivado (PV/PM/PE/Defesa), pericias,
 * derivados (deslocamento, carga, limites) e os "passos" do calculo (debug).
 */
public record ResultadoCalculo(
        Atributos atributosFinais,
        Status status,
        List<PericiaCalculada> pericias,
        int deslocamento,
        int cargaMaxima,
        int limiteHabilidades,
        int limiteFeiticos,
        int limiteBencaos,
        List<String> passos
) {}
