package com.asus.platform.web;

import com.asus.platform.domain.Criatura;
import com.asus.platform.domain.GameSystem;
import com.asus.platform.engine.AsusV1Engine;
import com.asus.platform.repository.CriaturaRepository;
import com.asus.platform.repository.GameSystemRepository;
import com.asus.platform.security.UsuarioPrincipal;
import com.asus.platform.service.DonoService;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/** Bestiario ASUS. */
@RestController
@RequestMapping("/api/bestiario")
public class BestiarioController {

    private final CriaturaRepository criaturaRepository;
    private final GameSystemRepository gameSystemRepository;

    private final DonoService donoService;

    public BestiarioController(CriaturaRepository criaturaRepository,
                               GameSystemRepository gameSystemRepository,
                               DonoService donoService) {
        this.criaturaRepository = criaturaRepository;
        this.gameSystemRepository = gameSystemRepository;
        this.donoService = donoService;
    }

    @GetMapping
    public List<Criatura> listar() {
        return criaturaRepository.findByGameSystemId(asus());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Criatura criar(@RequestBody Criatura c, @AuthenticationPrincipal UsuarioPrincipal principal) {
        if (c.getNome() == null || c.getNome().isBlank()) {
            throw new IllegalArgumentException("nome da criatura e obrigatorio");
        }
        c.setId(null);
        c.setGameSystemId(asus());
        c.setOficial(false);
        c.setCriadorId(principal != null ? principal.id() : null);
        return criaturaRepository.save(c);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void apagar(@PathVariable Long id, @AuthenticationPrincipal UsuarioPrincipal principal) {
        Criatura c = criaturaRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Criatura " + id + " nao encontrada"));
        // Anonimo (dev aberto) nao e bloqueado; logado: so o criador ou o dono do site.
        if (principal != null && !donoService.ehDono(principal.id())
                && !principal.id().equals(c.getCriadorId())) {
            throw new AcessoNegadoException("Só quem criou a criatura (ou o dono do site) pode apagá-la.");
        }
        criaturaRepository.deleteById(id);
    }

    private Long asus() {
        return gameSystemRepository.findByCodigo(AsusV1Engine.SYSTEM_ID)
                .map(GameSystem::getId)
                .orElseThrow(() -> new NotFoundException("Sistema ASUS nao encontrado"));
    }
}
