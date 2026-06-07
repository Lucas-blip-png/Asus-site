package com.asus.platform.web;

import com.asus.platform.service.EscudoService;
import com.asus.platform.web.dto.AtualizarStatusRequest;
import com.asus.platform.web.dto.EscudoResponse;
import com.asus.platform.web.dto.PersonagemResponse;
import com.asus.platform.web.dto.RolagemResponse;
import java.util.List;
import org.springframework.web.bind.annotation.*;

/** Escudo do Mestre (plano, Fase 8). */
@RestController
@RequestMapping("/api/campanhas/{id}/escudo")
public class EscudoController {

    private final EscudoService service;

    public EscudoController(EscudoService service) {
        this.service = service;
    }

    @GetMapping
    public EscudoResponse escudo(@PathVariable Long id,
                                 @RequestParam(required = false) Long usuarioId) {
        return service.escudo(id, usuarioId);
    }

    @GetMapping("/rolagens")
    public List<RolagemResponse> rolagens(@PathVariable Long id,
                                          @RequestParam(required = false) Long usuarioId) {
        return service.rolagensCompletas(id, usuarioId);
    }

    @PatchMapping("/personagens/{personagemId}/status")
    public PersonagemResponse editarStatus(@PathVariable Long id,
                                           @PathVariable Long personagemId,
                                           @RequestParam(required = false) Long usuarioId,
                                           @RequestBody AtualizarStatusRequest req) {
        return service.editarStatus(id, personagemId, usuarioId, req.pvAtual(), req.pmAtual(), req.peAtual());
    }
}
