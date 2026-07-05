package com.asus.platform;

import static org.assertj.core.api.Assertions.assertThat;

import com.asus.platform.domain.Atributos;
import com.asus.platform.domain.Classe;
import com.asus.platform.domain.Pericia;
import com.asus.platform.domain.Raca;
import com.asus.platform.engine.AsusV1Engine;
import com.asus.platform.engine.ContextoCalculo;
import com.asus.platform.engine.PericiaCalculada;
import com.asus.platform.engine.ResultadoCalculo;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class AsusV1EngineTest {

    private final AsusV1Engine engine = new AsusV1Engine(new ObjectMapper());

    @Test
    void calculaFichaComBonusDeClasse() {
        Raca humano = Raca.builder()
                .nome("Humano").codigo("HUMANO")
                .pvBase(5).pmBase(5).peBase(5)
                .build();

        // Cavaleiro: PV/PM/PE 6/3/6; +2 Forca, +2 Constituicao, +1 Carisma; treino +2 Vigor
        Classe cavaleiro = Classe.builder()
                .nome("Cavaleiro").codigo("CAVALEIRO")
                .multiplicadorPv(6).multiplicadorPm(3).multiplicadorPe(6)
                .jsonBonus("{\"atributos\":{\"forca\":2,\"constituicao\":2,\"carisma\":1},"
                        + "\"pericias\":{\"vigor\":2}}")
                .build();

        Atributos base = Atributos.builder()
                .forca(3).constituicao(4).destreza(2).agilidade(3)
                .inteligencia(1).sabedoria(1).carisma(2)
                .build();

        Pericia vigor = Pericia.builder()
                .codigo("VIGOR").nome("Vigor").atributoBase("CONSTITUICAO").build();
        Pericia combate = Pericia.builder()
                .codigo("COMBATE").nome("Combate").atributoBase("FORCA").build();

        ResultadoCalculo r = engine.calcular(new ContextoCalculo(
                humano, List.of(cavaleiro), base, 1, List.of(vigor, combate), Map.of()));

        // Finais: Forca 5, Constituicao 6, Carisma 3
        assertThat(r.atributosFinais().getForca()).isEqualTo(5);
        assertThat(r.atributosFinais().getConstituicao()).isEqualTo(6);
        assertThat(r.atributosFinais().getCarisma()).isEqualTo(3);

        // PV = racaPV(5) + classePV(6) + Con(6)*2 = 23 ; PM = 5 + 3 + Int(1)*2 = 10 ; PE = 5 + 6 + Con(6)*2 = 23
        assertThat(r.status().getPvMax()).isEqualTo(23);
        assertThat(r.status().getPvAtual()).isEqualTo(23);
        assertThat(r.status().getPmMax()).isEqualTo(10);
        assertThat(r.status().getPeMax()).isEqualTo(23);
        // Deslocamento = 4 + Agi(3)/5 = 4 ; Carga = For(5)*2 = 10
        assertThat(r.deslocamento()).isEqualTo(4);
        assertThat(r.cargaMaxima()).isEqualTo(10);

        // Vigor: treino do jogador 0, bonus fixo de classe +2 (separado, nao editavel), cap = 2*Con(6) = 12
        PericiaCalculada vig = acharPericia(r, "VIGOR");
        assertThat(vig.treino()).isEqualTo(0);
        assertThat(vig.bonus()).isEqualTo(2);
        assertThat(vig.cap()).isEqualTo(12);
        assertThat(vig.sigla()).isEqualTo("Con");

        // Combate: sem treino nem bonus, cap = 2*For(5) = 10
        PericiaCalculada com = acharPericia(r, "COMBATE");
        assertThat(com.treino()).isEqualTo(0);
        assertThat(com.bonus()).isEqualTo(0);
        assertThat(com.cap()).isEqualTo(10);

        assertThat(r.passos()).isNotEmpty();
    }

    private PericiaCalculada acharPericia(ResultadoCalculo r, String codigo) {
        return r.pericias().stream()
                .filter(p -> p.codigo().equals(codigo))
                .findFirst().orElseThrow();
    }
}
