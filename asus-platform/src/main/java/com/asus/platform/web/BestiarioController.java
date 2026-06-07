package com.asus.platform.web;

import com.asus.platform.domain.Criatura;
import com.asus.platform.domain.GameSystem;
import com.asus.platform.engine.AsusV1Engine;
import com.asus.platform.repository.CriaturaRepository;
import com.asus.platform.repository.GameSystemRepository;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

/** Bestiario ASUS. */
@RestController
@RequestMapping("/api/bestiario")
public class BestiarioController {

    private final CriaturaRepository criaturaRepository;
    private final GameSystemRepository gameSystemRepository;

    public BestiarioController(CriaturaRepository criaturaRepository,
                               GameSystemRepository gameSystemRepository) {
        this.criaturaRepository = criaturaRepository;
        this.gameSystemRepository = gameSystemRepository;
    }

    @GetMapping
    public List<Criatura> listar() {
        return criaturaRepository.findByGameSystemId(asus());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Criatura criar(@RequestBody Criatura c) {
        if (c.getNome() == null || c.getNome().isBlank()) {
            throw new IllegalArgumentException("nome da criatura e obrigatorio");
        }
        c.setId(null);
        c.setGameSystemId(asus());
        c.setOficial(false);
        return criaturaRepository.save(c);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void apagar(@PathVariable Long id) {
        criaturaRepository.deleteById(id);
    }

    private Long asus() {
        return gameSystemRepository.findByCodigo(AsusV1Engine.SYSTEM_ID)
                .map(GameSystem::getId)
                .orElseThrow(() -> new NotFoundException("Sistema ASUS nao encontrado"));
    }
}
