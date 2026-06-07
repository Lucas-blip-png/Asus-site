package com.asus.platform.web;

import com.asus.platform.domain.Classe;
import com.asus.platform.domain.GameSystem;
import com.asus.platform.domain.Pericia;
import com.asus.platform.domain.Raca;
import com.asus.platform.engine.AsusV1Engine;
import com.asus.platform.repository.ClasseRepository;
import com.asus.platform.repository.GameSystemRepository;
import com.asus.platform.repository.PericiaRepository;
import com.asus.platform.repository.RacaRepository;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Exposicao das regras do sistema (plano, secoes 2.4 e 21.2). */
@RestController
@RequestMapping("/api/sistemas")
public class SistemaController {

    private final GameSystemRepository gameSystemRepository;
    private final RacaRepository racaRepository;
    private final ClasseRepository classeRepository;
    private final PericiaRepository periciaRepository;

    public SistemaController(GameSystemRepository gameSystemRepository,
                             RacaRepository racaRepository,
                             ClasseRepository classeRepository,
                             PericiaRepository periciaRepository) {
        this.gameSystemRepository = gameSystemRepository;
        this.racaRepository = racaRepository;
        this.classeRepository = classeRepository;
        this.periciaRepository = periciaRepository;
    }

    @GetMapping
    public List<GameSystem> listar() {
        return gameSystemRepository.findAll();
    }

    @GetMapping("/asus")
    public GameSystem asus() {
        return asusSystem();
    }

    @GetMapping("/asus/racas")
    public List<Raca> racas() {
        return racaRepository.findByGameSystemId(asusSystem().getId());
    }

    @GetMapping("/asus/classes")
    public List<Classe> classes() {
        return classeRepository.findByGameSystemId(asusSystem().getId());
    }

    @GetMapping("/asus/pericias")
    public List<Pericia> pericias() {
        return periciaRepository.findByGameSystemId(asusSystem().getId());
    }

    private GameSystem asusSystem() {
        return gameSystemRepository.findByCodigo(AsusV1Engine.SYSTEM_ID)
                .orElseThrow(() -> new NotFoundException("Sistema ASUS nao encontrado"));
    }
}
