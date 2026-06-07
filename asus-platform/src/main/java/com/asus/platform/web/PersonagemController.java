package com.asus.platform.web;

import com.asus.platform.service.PersonagemService;
import com.asus.platform.web.dto.AtualizarPersonagemRequest;
import com.asus.platform.web.dto.AtualizarStatusRequest;
import com.asus.platform.web.dto.AuditoriaResponse;
import com.asus.platform.web.dto.CalculoDebugResponse;
import com.asus.platform.web.dto.CriarPersonagemRequest;
import com.asus.platform.web.dto.ExportPersonagemResponse;
import com.asus.platform.web.dto.ImportPersonagemRequest;
import com.asus.platform.web.dto.PersonagemResponse;
import com.asus.platform.web.dto.SnapshotResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

/** Personagens (plano, secao 21.3). */
@RestController
@RequestMapping("/api")
public class PersonagemController {

    private final PersonagemService service;

    public PersonagemController(PersonagemService service) {
        this.service = service;
    }

    @GetMapping("/organizacoes/{orgId}/personagens")
    public List<PersonagemResponse> listar(@PathVariable Long orgId) {
        return service.listar(orgId);
    }

    @PostMapping("/organizacoes/{orgId}/personagens")
    @ResponseStatus(HttpStatus.CREATED)
    public PersonagemResponse criar(@PathVariable Long orgId,
                                    @Valid @RequestBody CriarPersonagemRequest req) {
        return service.criar(orgId, req);
    }

    @GetMapping("/personagens/{id}")
    public PersonagemResponse buscar(@PathVariable Long id) {
        return service.buscar(id);
    }

    @PostMapping("/personagens/import")
    @ResponseStatus(HttpStatus.CREATED)
    public PersonagemResponse importar(@Valid @RequestBody ImportPersonagemRequest req) {
        return service.importar(req);
    }

    @PutMapping("/personagens/{id}")
    public PersonagemResponse atualizar(@PathVariable Long id,
                                        @Valid @RequestBody AtualizarPersonagemRequest req) {
        return service.atualizar(id, req);
    }

    @PatchMapping("/personagens/{id}/status")
    public PersonagemResponse atualizarStatus(@PathVariable Long id,
                                              @RequestBody AtualizarStatusRequest req) {
        return service.atualizarStatus(id, req.pvAtual(), req.pmAtual(), req.peAtual());
    }

    @GetMapping("/personagens/{id}/export")
    public ExportPersonagemResponse exportar(@PathVariable Long id) {
        return service.exportar(id);
    }

    @GetMapping("/personagens/{id}/debug")
    public CalculoDebugResponse debug(@PathVariable Long id) {
        return service.debug(id);
    }

    @GetMapping("/personagens/{id}/snapshots")
    public List<SnapshotResponse> snapshots(@PathVariable Long id) {
        return service.snapshots(id);
    }

    @GetMapping("/personagens/{id}/auditoria")
    public List<AuditoriaResponse> auditoria(@PathVariable Long id) {
        return service.historicoAuditoria(id);
    }
}
