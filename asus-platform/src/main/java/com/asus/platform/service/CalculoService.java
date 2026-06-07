package com.asus.platform.service;

import com.asus.platform.domain.Classe;
import com.asus.platform.domain.GameSystem;
import com.asus.platform.domain.Personagem;
import com.asus.platform.domain.Pericia;
import com.asus.platform.domain.Raca;
import com.asus.platform.engine.ContextoCalculo;
import com.asus.platform.engine.GameSystemEngine;
import com.asus.platform.engine.GameSystemRegistry;
import com.asus.platform.engine.ResultadoCalculo;
import com.asus.platform.repository.ClasseRepository;
import com.asus.platform.repository.GameSystemRepository;
import com.asus.platform.repository.PericiaRepository;
import com.asus.platform.repository.RacaRepository;
import com.asus.platform.web.NotFoundException;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * Calcula a ficha de um personagem (criterio de aceite 3).
 *
 * <p>O backend e a fonte unica das regras (plano, secao 2.4): resolve raca,
 * classe e pericias do sistema e delega o calculo ao engine correto.</p>
 */
@Service
public class CalculoService {

    private final GameSystemRepository gameSystemRepository;
    private final RacaRepository racaRepository;
    private final ClasseRepository classeRepository;
    private final PericiaRepository periciaRepository;
    private final GameSystemRegistry registry;

    public CalculoService(GameSystemRepository gameSystemRepository,
                          RacaRepository racaRepository,
                          ClasseRepository classeRepository,
                          PericiaRepository periciaRepository,
                          GameSystemRegistry registry) {
        this.gameSystemRepository = gameSystemRepository;
        this.racaRepository = racaRepository;
        this.classeRepository = classeRepository;
        this.periciaRepository = periciaRepository;
        this.registry = registry;
    }

    public ResultadoCalculo calcular(Personagem personagem) {
        GameSystem sistema = gameSystemRepository.findById(personagem.getGameSystemId())
                .orElseThrow(() -> new NotFoundException("Sistema nao encontrado"));

        Raca raca = racaRepository.findById(personagem.getRacaId())
                .orElseThrow(() -> new NotFoundException("Raca nao encontrada"));

        Classe classe = classeRepository.findById(personagem.getClasseId())
                .orElseThrow(() -> new NotFoundException("Classe nao encontrada"));

        List<Pericia> pericias = periciaRepository.findByGameSystemId(sistema.getId());

        GameSystemEngine engine = registry.getEngine(sistema.getCodigo(), personagem.getRulesetVersion());

        ContextoCalculo contexto = new ContextoCalculo(
                raca, classe, personagem.getAtributosBase(), personagem.getNivel(), pericias);

        return engine.calcular(contexto);
    }
}
