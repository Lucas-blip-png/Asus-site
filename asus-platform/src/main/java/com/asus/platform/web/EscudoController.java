package com.asus.platform.web;

import com.asus.platform.security.UsuarioPrincipal;
import com.asus.platform.service.EscudoService;
import com.asus.platform.web.dto.AtualizarStatusRequest;
import com.asus.platform.web.dto.EscudoResponse;
import com.asus.platform.web.dto.PersonagemResponse;
import com.asus.platform.web.dto.RolagemResponse;
import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/** Escudo do Mestre (plano, Fase 8). O usuario vem do JWT; a query so vale no modo aberto/dev. */
@RestController
@RequestMapping("/api/campanhas/{id}/escudo")
public class EscudoController {

    private final EscudoService service;

    public EscudoController(EscudoService service) {
        this.service = service;
    }

    private static Long uid(UsuarioPrincipal principal, Long usuarioIdQuery) {
        return principal != null ? principal.id() : usuarioIdQuery;
    }

    @GetMapping
    public EscudoResponse escudo(@PathVariable Long id,
                                 @RequestParam(required = false) Long usuarioId,
                                 @AuthenticationPrincipal UsuarioPrincipal principal) {
        return service.escudo(id, uid(principal, usuarioId));
    }

    @GetMapping("/rolagens")
    public List<RolagemResponse> rolagens(@PathVariable Long id,
                                          @RequestParam(required = false) Long usuarioId,
                                          @AuthenticationPrincipal UsuarioPrincipal principal) {
        return service.rolagensCompletas(id, uid(principal, usuarioId));
    }

    @PatchMapping("/personagens/{personagemId}/status")
    public PersonagemResponse editarStatus(@PathVariable Long id,
                                           @PathVariable Long personagemId,
                                           @RequestParam(required = false) Long usuarioId,
                                           @AuthenticationPrincipal UsuarioPrincipal principal,
                                           @RequestBody AtualizarStatusRequest req) {
        return service.editarStatus(id, personagemId, uid(principal, usuarioId), req.pvAtual(), req.pmAtual(), req.peAtual());
    }
}
