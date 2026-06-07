package com.asus.platform.engine;

import com.asus.platform.domain.Atributos;
import com.asus.platform.domain.Classe;
import com.asus.platform.domain.Pericia;
import com.asus.platform.domain.Raca;
import java.util.List;
import java.util.Map;

/**
 * Tudo que o engine precisa para calcular uma ficha.
 *
 * @param fontesBonus classe primaria + trilha (se houver); cada uma aplica seu jsonBonus.
 * @param periciasTreino pontos de treino por codigo de pericia (escolhidos pelo jogador).
 */
public record ContextoCalculo(
        Raca raca,
        List<Classe> fontesBonus,
        Atributos atributosBase,
        int nivel,
        List<Pericia> periciasDoSistema,
        Map<String, Integer> periciasTreino
) {}
