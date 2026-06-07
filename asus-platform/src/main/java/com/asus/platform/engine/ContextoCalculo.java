package com.asus.platform.engine;

import com.asus.platform.domain.Atributos;
import com.asus.platform.domain.Classe;
import com.asus.platform.domain.Pericia;
import com.asus.platform.domain.Raca;
import java.util.List;

/** Tudo que o engine precisa para calcular uma ficha, sem depender da entidade Personagem. */
public record ContextoCalculo(
        Raca raca,
        Classe classe,
        Atributos atributosBase,
        int nivel,
        List<Pericia> periciasDoSistema
) {}
