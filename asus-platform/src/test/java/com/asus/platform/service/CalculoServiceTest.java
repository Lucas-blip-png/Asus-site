package com.asus.platform.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.asus.platform.domain.Atributos;
import com.asus.platform.domain.Classe;
import com.asus.platform.domain.GameSystem;
import com.asus.platform.domain.Personagem;
import com.asus.platform.domain.Raca;
import com.asus.platform.engine.ContextoCalculo;
import com.asus.platform.engine.GameSystemEngine;
import com.asus.platform.engine.GameSystemRegistry;
import com.asus.platform.repository.ClasseRepository;
import com.asus.platform.repository.GameSystemRepository;
import com.asus.platform.repository.PericiaRepository;
import com.asus.platform.repository.RacaRepository;
import com.asus.platform.web.NotFoundException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

/** Montagem do contexto de calculo: fontes de bonus, penalidade de carga e treino de pericias. */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CalculoServiceTest {

    @Mock GameSystemRepository sistemas;
    @Mock RacaRepository racas;
    @Mock ClasseRepository classes;
    @Mock PericiaRepository pericias;
    @Mock GameSystemRegistry registry;
    @Mock GameSystemEngine engine;

    CalculoService calculo;

    private final Classe guerreiro = Classe.builder().id(20L).build();
    private final Classe trilha = Classe.builder().id(21L).build();
    private final Classe secundaria = Classe.builder().id(30L).build();

    @BeforeEach
    void setUp() {
        calculo = new CalculoService(sistemas, racas, classes, pericias, registry, new ObjectMapper());
        when(sistemas.findById(1L)).thenReturn(Optional.of(GameSystem.builder().id(1L).codigo("ASUS").build()));
        when(racas.findById(2L)).thenReturn(Optional.of(Raca.builder().id(2L).build()));
        when(classes.findById(20L)).thenReturn(Optional.of(guerreiro));
        when(classes.findById(21L)).thenReturn(Optional.of(trilha));
        when(classes.findById(30L)).thenReturn(Optional.of(secundaria));
        when(pericias.findByGameSystemId(1L)).thenReturn(List.of());
        when(registry.getEngine(any(), any())).thenReturn(engine);
    }

    private static Personagem.PersonagemBuilder personagem() {
        return Personagem.builder().gameSystemId(1L).racaId(2L).classeId(20L).nivel(5)
                .atributosBase(Atributos.builder().forca(3).constituicao(2).destreza(1)
                        .agilidade(4).inteligencia(0).sabedoria(1).carisma(2).build());
    }

    private ContextoCalculo contextoEnviadoAoEngine() {
        ArgumentCaptor<ContextoCalculo> captor = ArgumentCaptor.forClass(ContextoCalculo.class);
        verify(engine).calcular(captor.capture());
        return captor.getValue();
    }

    @Test
    void penalidadeDeCargaTiraAgilidadeSemMexerNosOutrosAtributos() {
        calculo.calcular(personagem().build(), 3);

        Atributos base = contextoEnviadoAoEngine().atributosBase();
        assertThat(base.getAgilidade()).isEqualTo(1);
        assertThat(base.getForca()).isEqualTo(3);
        assertThat(base.getCarisma()).isEqualTo(2);
    }

    @Test
    void semPenalidadeUsaOsAtributosOriginais() {
        Personagem p = personagem().build();
        calculo.calcular(p);

        assertThat(contextoEnviadoAoEngine().atributosBase()).isSameAs(p.getAtributosBase());
    }

    @Test
    void trilhaEMulticlasseEntramComoFontesDeBonus() {
        calculo.calcular(personagem().trilhaId(21L).classeSecundariaId(30L).build());

        assertThat(contextoEnviadoAoEngine().fontesBonus()).containsExactly(guerreiro, trilha, secundaria);
    }

    @Test
    void treinoDePericiasEhNormalizadoParaMaiusculas() {
        calculo.calcular(personagem().jsonPericias("{\"luta\": 2, \"Furtividade\": 1}").build());

        assertThat(contextoEnviadoAoEngine().periciasTreino())
                .containsEntry("LUTA", 2)
                .containsEntry("FURTIVIDADE", 1);
    }

    @Test
    void jsonDeTreinoInvalidoViraFichaSemTreino() {
        calculo.calcular(personagem().jsonPericias("{quebrado").build());

        assertThat(contextoEnviadoAoEngine().periciasTreino()).isEmpty();
    }

    @Test
    void racaInexistenteDa404() {
        assertThatThrownBy(() -> calculo.calcular(personagem().racaId(999L).build()))
                .isInstanceOf(NotFoundException.class);
    }
}
