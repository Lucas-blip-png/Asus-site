package com.asus.platform.web;

import com.asus.platform.service.TemplateService;
import com.asus.platform.web.dto.CriarTemplateRequest;
import com.asus.platform.web.dto.TemplateResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

/** Templates (plano, Seção 15 / Fase 12). */
@RestController
@RequestMapping("/api")
public class TemplateController {

    private final TemplateService service;

    public TemplateController(TemplateService service) {
        this.service = service;
    }

    @GetMapping("/organizacoes/{orgId}/templates")
    public List<TemplateResponse> listar(@PathVariable Long orgId) {
        return service.listar(orgId);
    }

    @PostMapping("/organizacoes/{orgId}/templates")
    @ResponseStatus(HttpStatus.CREATED)
    public TemplateResponse criar(@PathVariable Long orgId,
                                  @Valid @RequestBody CriarTemplateRequest req) {
        return service.criar(orgId, req);
    }

    @GetMapping("/templates/publicos")
    public List<TemplateResponse> publicos() {
        return service.listarPublicos();
    }

    @GetMapping("/templates/{id}")
    public TemplateResponse buscar(@PathVariable Long id) {
        return service.buscar(id);
    }

    @DeleteMapping("/templates/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void apagar(@PathVariable Long id) {
        service.apagar(id);
    }
}
