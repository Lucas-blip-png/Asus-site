package com.asus.platform;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.asus.platform.engine.Dado;
import com.asus.platform.engine.ExpressaoDado;
import com.asus.platform.engine.ResultadoDado;
import java.util.ArrayDeque;
import java.util.Deque;
import org.junit.jupiter.api.Test;

class ExpressaoDadoTest {

    /** Dado deterministico: devolve as faces programadas em sequencia. */
    private static Dado dadoFixo(int... faces) {
        Deque<Integer> fila = new ArrayDeque<>();
        for (int f : faces) {
            fila.add(f);
        }
        return f -> fila.poll();
    }

    @Test
    void parseFormatoCompleto() {
        ExpressaoDado e = ExpressaoDado.parse("1d20+5");
        assertThat(e.quantidade()).isEqualTo(1);
        assertThat(e.faces()).isEqualTo(20);
        assertThat(e.modificador()).isEqualTo(5);
        assertThat(e.canonico()).isEqualTo("1d20+5");
    }

    @Test
    void parseAceitaVariacoes() {
        assertThat(ExpressaoDado.parse("d6")).isEqualTo(new ExpressaoDado(1, 6, 0));
        assertThat(ExpressaoDado.parse("3D8")).isEqualTo(new ExpressaoDado(3, 8, 0));
        assertThat(ExpressaoDado.parse(" 2d6 - 1 ")).isEqualTo(new ExpressaoDado(2, 6, -1));
        assertThat(ExpressaoDado.parse("2d6-1").canonico()).isEqualTo("2d6-1");
    }

    @Test
    void rolarSomaFacesEModificador() {
        ResultadoDado r = ExpressaoDado.parse("2d6+3").rolar(dadoFixo(4, 5));
        assertThat(r.dados()).containsExactly(4, 5);
        assertThat(r.modificador()).isEqualTo(3);
        assertThat(r.total()).isEqualTo(12);
    }

    @Test
    void parseRejeitaExpressoesInvalidas() {
        assertThatThrownBy(() -> ExpressaoDado.parse("abc"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ExpressaoDado.parse(""))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ExpressaoDado.parse("0d6"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ExpressaoDado.parse("1d1"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
