package com.asus.platform.service;

import com.asus.platform.domain.LimitesPlano;
import com.asus.platform.domain.Organizacao;
import com.asus.platform.domain.PapelCampanha;
import com.asus.platform.domain.Plano;
import com.asus.platform.repository.CampanhaMembroRepository;
import com.asus.platform.repository.CampanhaRepository;
import com.asus.platform.repository.OrganizacaoRepository;
import com.asus.platform.repository.PersonagemRepository;
import com.asus.platform.web.LimiteExcedidoException;
import org.springframework.stereotype.Service;

/** Aplica os limites do plano da organizacao (plano, Seção 4 / Fase 10). */
@Service
public class PlanoService {

    private final OrganizacaoRepository organizacaoRepository;
    private final PersonagemRepository personagemRepository;
    private final CampanhaRepository campanhaRepository;
    private final CampanhaMembroRepository campanhaMembroRepository;

    public PlanoService(OrganizacaoRepository organizacaoRepository,
                        PersonagemRepository personagemRepository,
                        CampanhaRepository campanhaRepository,
                        CampanhaMembroRepository campanhaMembroRepository) {
        this.organizacaoRepository = organizacaoRepository;
        this.personagemRepository = personagemRepository;
        this.campanhaRepository = campanhaRepository;
        this.campanhaMembroRepository = campanhaMembroRepository;
    }

    public Plano planoDa(Long organizacaoId) {
        return organizacaoRepository.findById(organizacaoId)
                .map(Organizacao::getPlano).orElse(Plano.FREE);
    }

    public LimitesPlano limitesDa(Long organizacaoId) {
        return LimitesPlano.de(planoDa(organizacaoId));
    }

    public void validarNovoPersonagem(Long organizacaoId) {
        LimitesPlano l = limitesDa(organizacaoId);
        long atual = personagemRepository.countByOrganizacaoIdAndArquivadoFalse(organizacaoId);
        if (atual >= l.maxPersonagens()) {
            throw new LimiteExcedidoException(
                    "Limite de personagens do plano atingido (" + l.maxPersonagens() + ")");
        }
    }

    public void validarNovaCampanha(Long organizacaoId) {
        LimitesPlano l = limitesDa(organizacaoId);
        long atual = campanhaRepository.countByOrganizacaoIdAndArquivadaFalse(organizacaoId);
        if (atual >= l.maxCampanhas()) {
            throw new LimiteExcedidoException(
                    "Limite de campanhas do plano atingido (" + l.maxCampanhas() + ")");
        }
    }

    public void validarNovoJogador(Long campanhaId, Long organizacaoId) {
        LimitesPlano l = limitesDa(organizacaoId);
        long atual = campanhaMembroRepository.countByCampanhaIdAndPapel(campanhaId, PapelCampanha.JOGADOR);
        if (atual >= l.maxJogadoresPorCampanha()) {
            throw new LimiteExcedidoException(
                    "Limite de jogadores por campanha do plano atingido (" + l.maxJogadoresPorCampanha() + ")");
        }
    }
}
