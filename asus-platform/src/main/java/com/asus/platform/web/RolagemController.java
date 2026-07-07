package com.asus.platform.web;

import com.asus.platform.security.UsuarioPrincipal;
import com.asus.platform.service.RolagemService;
import com.asus.platform.web.dto.RolagemResponse;
import com.asus.platform.web.dto.RolarRequest;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/** Rolagens de uma campanha (plano, secao 21.5). */
@RestController
@RequestMapping("/api/campanhas/{id}/rolagens")
public class RolagemController {

    private final RolagemService service;

    public RolagemController(RolagemService service) {
        this.service = service;
    }

    @GetMapping
    public List<RolagemResponse> listar(@PathVariable Long id,
                                        @AuthenticationPrincipal UsuarioPrincipal principal) {
        return service.listar(id, principal != null ? principal.id() : null);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RolagemResponse rolar(@PathVariable Long id, @Valid @RequestBody RolarRequest req) {
        return service.rolar(id, req);
    }

    /** Revela uma rolagem oculta. O usuario vem do JWT (não do cliente); a query só vale no modo aberto/dev. */
    @PostMapping("/{rolagemId}/revelar")
    public RolagemResponse revelar(@PathVariable Long id,
                                   @PathVariable Long rolagemId,
                                   @RequestParam(required = false) Long usuarioId,
                                   @AuthenticationPrincipal UsuarioPrincipal principal) {
        return service.revelar(id, rolagemId, principal != null ? principal.id() : usuarioId);
    }
}
