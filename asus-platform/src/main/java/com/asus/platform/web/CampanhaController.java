package com.asus.platform.web;

import com.asus.platform.security.UsuarioPrincipal;
import com.asus.platform.service.CampanhaService;
import com.asus.platform.web.dto.AdicionarPersonagemCampanhaRequest;
import com.asus.platform.web.dto.AtualizarCampanhaRequest;
import com.asus.platform.web.dto.CampanhaMembroResponse;
import com.asus.platform.web.dto.CampanhaPersonagemResponse;
import com.asus.platform.web.dto.CampanhaResponse;
import com.asus.platform.web.dto.ConviteResponse;
import com.asus.platform.web.dto.CriarCampanhaRequest;
import com.asus.platform.web.dto.CriarConviteRequest;
import com.asus.platform.web.dto.EntrarCampanhaRequest;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/** Campanhas (plano, secao 21.4). */
@RestController
@RequestMapping("/api")
public class CampanhaController {

    private final CampanhaService service;

    public CampanhaController(CampanhaService service) {
        this.service = service;
    }

    @GetMapping("/organizacoes/{orgId}/campanhas")
    public List<CampanhaResponse> listar(@PathVariable Long orgId) {
        return service.listar(orgId);
    }

    /** Campanhas onde o usuario logado e membro (mestre/jogador), de qualquer org. */
    @GetMapping("/campanhas/minhas")
    public List<CampanhaResponse> minhas(@AuthenticationPrincipal UsuarioPrincipal principal) {
        if (principal == null) {
            return List.of();
        }
        return service.listarDoUsuario(principal.id());
    }

    @PostMapping("/organizacoes/{orgId}/campanhas")
    @ResponseStatus(HttpStatus.CREATED)
    public CampanhaResponse criar(@PathVariable Long orgId,
                                  @AuthenticationPrincipal UsuarioPrincipal principal,
                                  @Valid @RequestBody CriarCampanhaRequest req) {
        return service.criar(orgId, principal != null ? principal.id() : null, req);
    }

    @GetMapping("/campanhas/{id}")
    public CampanhaResponse buscar(@PathVariable Long id) {
        return service.buscar(id);
    }

    @PutMapping("/campanhas/{id}")
    public CampanhaResponse atualizar(@PathVariable Long id,
                                      @AuthenticationPrincipal UsuarioPrincipal principal,
                                      @RequestBody AtualizarCampanhaRequest req) {
        if (principal != null) {
            service.exigirPermissao(id, principal.id(), com.asus.platform.domain.Permissao.GERENCIAR_CAMPANHA);
        }
        return service.atualizar(id, req);
    }

    @DeleteMapping("/campanhas/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void apagar(@PathVariable Long id,
                       @AuthenticationPrincipal UsuarioPrincipal principal) {
        if (principal != null) {
            service.exigirPermissao(id, principal.id(), com.asus.platform.domain.Permissao.GERENCIAR_CAMPANHA);
        }
        service.apagar(id);
    }

    private static Long uid(UsuarioPrincipal principal, Long usuarioIdQuery) {
        return principal != null ? principal.id() : usuarioIdQuery;
    }

    /** Anotações privadas do mestre (gateadas por permissão; não saem no CampanhaResponse). */
    @GetMapping("/campanhas/{id}/anotacoes")
    public Map<String, String> obterAnotacoes(@PathVariable Long id,
                                              @RequestParam(required = false) Long usuarioId,
                                              @AuthenticationPrincipal UsuarioPrincipal principal) {
        return Map.of("anotacoes", Optional.ofNullable(service.obterAnotacoes(id, uid(principal, usuarioId))).orElse(""));
    }

    @PutMapping("/campanhas/{id}/anotacoes")
    public Map<String, String> salvarAnotacoes(@PathVariable Long id,
                                               @RequestParam(required = false) Long usuarioId,
                                               @AuthenticationPrincipal UsuarioPrincipal principal,
                                               @RequestBody Map<String, String> body) {
        return Map.of("anotacoes",
                Optional.ofNullable(service.salvarAnotacoes(id, uid(principal, usuarioId), body.get("anotacoes"))).orElse(""));
    }

    @GetMapping("/campanhas/{id}/personagens")
    public List<CampanhaPersonagemResponse> personagens(@PathVariable Long id) {
        return service.listarPersonagens(id);
    }

    @PostMapping("/campanhas/{id}/personagens")
    @ResponseStatus(HttpStatus.CREATED)
    public CampanhaPersonagemResponse adicionarPersonagem(
            @PathVariable Long id,
            @Valid @RequestBody AdicionarPersonagemCampanhaRequest req) {
        return service.adicionarPersonagem(id, req.personagemId());
    }

    @GetMapping("/campanhas/{id}/membros")
    public List<CampanhaMembroResponse> membros(@PathVariable Long id) {
        return service.listarMembros(id);
    }

    @PostMapping("/campanhas/{id}/convites")
    @ResponseStatus(HttpStatus.CREATED)
    public ConviteResponse criarConvite(@PathVariable Long id,
                                        @RequestBody(required = false) CriarConviteRequest req) {
        return service.criarConvite(id, req == null
                ? new CriarConviteRequest(null, null, null, null) : req);
    }

    @PostMapping("/campanhas/entrar/{codigo}")
    public CampanhaMembroResponse entrar(@PathVariable String codigo,
                                         @Valid @RequestBody EntrarCampanhaRequest req) {
        return service.entrar(codigo, req.usuarioId());
    }
}
