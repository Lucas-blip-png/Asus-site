package com.asus.platform.web;

import com.asus.platform.service.OrganizacaoService;
import com.asus.platform.web.dto.CriarOrganizacaoRequest;
import com.asus.platform.web.dto.OrganizacaoResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

/** CRUD minimo de organizacoes (plano, secao 21.1). */
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

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OrganizacaoResponse criar(@Valid @RequestBody CriarOrganizacaoRequest req) {
        return OrganizacaoResponse.de(service.criar(req.nome(), req.slug()));
    }

    @GetMapping("/{id}")
    public OrganizacaoResponse buscar(@PathVariable Long id) {
        return OrganizacaoResponse.de(service.buscar(id));
    }
}
