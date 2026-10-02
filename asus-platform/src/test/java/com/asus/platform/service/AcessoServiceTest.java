package com.asus.platform.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.asus.platform.domain.Campanha;
import com.asus.platform.domain.CampanhaPersonagem;
import com.asus.platform.domain.Personagem;
import com.asus.platform.repository.CampanhaPersonagemRepository;
import com.asus.platform.repository.CampanhaRepository;
import com.asus.platform.repository.PersonagemRepository;
import com.asus.platform.security.UsuarioPrincipal;
import com.asus.platform.web.AcessoNegadoException;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

/** Regras de autorizacao da ficha: dono, mestre da campanha, dono do site e modo aberto. */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AcessoServiceTest {

    private static final long PERSONAGEM = 10L;
    private static final long DONO_FICHA = 1L;
    private static final long MESTRE = 2L;
    private static final long OUTRO_JOGADOR = 3L;
    private static final long ADMIN = 99L;

    @Mock PersonagemRepository personagens;
    @Mock CampanhaPersonagemRepository vinculos;
    @Mock CampanhaRepository campanhas;
    @Mock DonoService donoService;
    @InjectMocks AcessoService acesso;

    @BeforeEach
    void fichaVinculadaAUmaCampanha() {
        when(personagens.findById(PERSONAGEM))
                .thenReturn(Optional.of(Personagem.builder().id(PERSONAGEM).usuarioId(DONO_FICHA).build()));
        when(vinculos.findByPersonagemId(PERSONAGEM))
                .thenReturn(List.of(CampanhaPersonagem.builder().campanhaId(5L).personagemId(PERSONAGEM).build()));
        when(campanhas.findById(5L))
                .thenReturn(Optional.of(Campanha.builder().id(5L).mestreId(MESTRE).build()));
        when(donoService.ehDono(ADMIN)).thenReturn(true);
    }

    private static UsuarioPrincipal usuario(long id) {
        return new UsuarioPrincipal(id, "u" + id + "@test.local");
    }

    @Test
    void modoAbertoSemUsuarioNaoBloqueia() {
        assertThatCode(() -> acesso.exigirDonoOuMestrePersonagem(PERSONAGEM, null)).doesNotThrowAnyException();
        verifyNoInteractions(personagens);
    }

    @Test
    void donoDaFichaAcessa() {
        assertThatCode(() -> acesso.exigirDonoOuMestrePersonagem(PERSONAGEM, usuario(DONO_FICHA)))
                .doesNotThrowAnyException();
    }

    @Test
    void mestreDaCampanhaDoPersonagemAcessa() {
        assertThatCode(() -> acesso.exigirDonoOuMestrePersonagem(PERSONAGEM, usuario(MESTRE)))
                .doesNotThrowAnyException();
        assertThat(acesso.ehMestreDoPersonagem(PERSONAGEM, MESTRE)).isTrue();
    }

    @Test
    void outroJogadorNaoVeFichaPrivada() {
        assertThatThrownBy(() -> acesso.exigirDonoOuMestrePersonagem(PERSONAGEM, usuario(OUTRO_JOGADOR)))
                .isInstanceOf(AcessoNegadoException.class);
    }

    @Test
    void donoDoSiteAcessaTudo() {
        assertThatCode(() -> acesso.exigirDonoPersonagem(PERSONAGEM, usuario(ADMIN))).doesNotThrowAnyException();
    }

    @Test
    void mestreNaoEditaFichaSemSerOMestreInformado() {
        // exigirDonoPersonagem nao considera mestre: so o dono edita fora do fluxo de campanha
        assertThatThrownBy(() -> acesso.exigirDonoPersonagem(PERSONAGEM, usuario(MESTRE)))
                .isInstanceOf(AcessoNegadoException.class);
        assertThatCode(() -> acesso.exigirDonoPersonagemOuMestre(PERSONAGEM, MESTRE, usuario(MESTRE)))
                .doesNotThrowAnyException();
    }

    @Test
    void personagemInexistenteNaoVazaAcessoNegado() {
        when(personagens.findById(404L)).thenReturn(Optional.empty());
        assertThatCode(() -> acesso.exigirDonoPersonagem(404L, usuario(OUTRO_JOGADOR))).doesNotThrowAnyException();
    }
}
