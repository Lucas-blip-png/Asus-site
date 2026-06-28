package com.asus.platform.web;

import com.asus.platform.domain.OrganizacaoMembro;
import com.asus.platform.security.UsuarioPrincipal;
import com.asus.platform.service.OrganizacaoService;
import com.asus.platform.web.dto.AdicionarMembroRequest;
import com.asus.platform.web.dto.AtualizarOrganizacaoRequest;
import com.asus.platform.web.dto.CriarOrganizacaoRequest;
import com.asus.platform.web.dto.OrganizacaoResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/** Organizacoes e membros (plano, secao 21.1). */
@RestController
@RequestMapping("/api/organizacoes")
public class OrganizacaoController {

    private final OrganizacaoService service;

    public OrganizacaoController(OrganizacaoService service) {
        this.service = service;
    }

    @GetMapping
    public List<OrganizacaoResponse> listar() {
        return service.listar().stream().map(OrganizacaoResponse::de).toList();
    }

    /** Organizacoes do usuario logado (contas individuais). Sem login, lista vazia. */
    @GetMapping("/minhas")
    public List<OrganizacaoResponse> minhas(@AuthenticationPrincipal UsuarioPrincipal principal) {
        if (principal == null) {
            return List.of();
        }
        return service.listarDoUsuario(principal.id()).stream().map(OrganizacaoResponse::de).toList();
    }

    /** Org pessoal do usuario (retorna-ou-cria). Estavel entre reloads: evita dados "sumirem". */
    @GetMapping("/minha")
    public OrganizacaoResponse minha(@AuthenticationPrincipal UsuarioPrincipal principal) {
        if (principal == null) {
            throw new IllegalArgumentException("Necessario estar logado");
        }
        return OrganizacaoResponse.de(service.minhaOuCriar(principal.id()));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OrganizacaoResponse criar(@AuthenticationPrincipal UsuarioPrincipal principal,
                                     @Valid @RequestBody CriarOrganizacaoRequest req) {
        return OrganizacaoResponse.de(service.criar(req.nome(), req.slug(),
                principal != null ? principal.id() : null));
    }

    @GetMapping("/{id}")
    public OrganizacaoResponse buscar(@PathVariable Long id) {
        return OrganizacaoResponse.de(service.buscar(id));
    }

    @PutMapping("/{id}")
    public OrganizacaoResponse atualizar(@PathVariable Long id,
                                         @RequestBody AtualizarOrganizacaoRequest req) {
        return OrganizacaoResponse.de(service.atualizar(id, req.nome()));
    }

    @GetMapping("/{id}/membros")
    public List<OrganizacaoMembro> membros(@PathVariable Long id) {
        return service.listarMembros(id);
    }

    @PostMapping("/{id}/membros")
    @ResponseStatus(HttpStatus.CREATED)
    public OrganizacaoMembro adicionarMembro(@PathVariable Long id,
                                             @Valid @RequestBody AdicionarMembroRequest req) {
        return service.adicionarMembro(id, req.usuarioId(), req.papel());
    }

    @DeleteMapping("/{id}/membros/{usuarioId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removerMembro(@PathVariable Long id, @PathVariable Long usuarioId) {
        service.removerMembro(id, usuarioId);
    }
}
