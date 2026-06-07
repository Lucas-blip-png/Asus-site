package com.asus.platform;

import static org.assertj.core.api.Assertions.assertThat;

import com.asus.platform.domain.Atributos;
import com.asus.platform.domain.Classe;
import com.asus.platform.domain.Pericia;
import com.asus.platform.domain.Raca;
import com.asus.platform.engine.AsusV1Engine;
import com.asus.platform.engine.ContextoCalculo;
import com.asus.platform.engine.ResultadoCalculo;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.junit.jupiter.api.Test;

class AsusV1EngineTest {

    private final AsusV1Engine engine = new AsusV1Engine(new ObjectMapper());

    @Test
    void calculaStatusAplicandoBonusRacial() {
        Raca raca = Raca.builder()
                .nome("Anao").codigo("ANAO")
                .pvBase(12).pmBase(6).peBase(4)
                .jsonHabilidades("{\"bonusAtributos\":{\"vigor\":2}}")
                .build();

        Classe classe = Classe.builder()
                .nome("Guerreiro").codigo("GUERREIRO")
                .multiplicadorPv(6).multiplicadorPm(1).multiplicadorPe(3)
                .build();

        Atributos base = Atributos.builder()
                .forca(2).agilidade(3).vigor(1).intelecto(2).presenca(1)
                .build();

        Pericia atletismo = Pericia.builder()
                .codigo("ATLETISMO").nome("Atletismo").atributoBase("FORCA").build();

        ResultadoCalculo r = engine.calcular(
                new ContextoCalculo(raca, classe, base, 1, List.of(atletismo)));

        // vigor final = 1 + 2 = 3
        assertThat(r.atributosFinais().getVigor()).isEqualTo(3);

        // PV = 12 + 6*1 + 3*2 = 24
        assertThat(r.status().getPvMax()).isEqualTo(24);
        assertThat(r.status().getPvAtual()).isEqualTo(24);
        // PM = 6 + 1*1 + 2*2 = 11
        assertThat(r.status().getPmMax()).isEqualTo(11);
        // PE = 4 + 3*1 + 3 = 10
        assertThat(r.status().getPeMax()).isEqualTo(10);
        // Defesa = 10 + agilidade(3) = 13
        assertThat(r.status().getDefesa()).isEqualTo(13);

        // Pericia Atletismo = forca(2) + nivel/2(0) = 2
        assertThat(r.pericias()).hasSize(1);
        assertThat(r.pericias().get(0).valor()).isEqualTo(2);

        // Debug deve registrar passos
        assertThat(r.passos()).isNotEmpty();
    }
}
