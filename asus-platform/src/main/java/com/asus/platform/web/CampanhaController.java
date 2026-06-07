package com.asus.platform.web;

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
import org.springframework.http.HttpStatus;
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

    @PostMapping("/organizacoes/{orgId}/campanhas")
    @ResponseStatus(HttpStatus.CREATED)
    public CampanhaResponse criar(@PathVariable Long orgId,
                                  @Valid @RequestBody CriarCampanhaRequest req) {
        return service.criar(orgId, req);
    }

    @GetMapping("/campanhas/{id}")
    public CampanhaResponse buscar(@PathVariable Long id) {
        return service.buscar(id);
    }

    @PutMapping("/campanhas/{id}")
    public CampanhaResponse atualizar(@PathVariable Long id,
                                      @RequestBody AtualizarCampanhaRequest req) {
        return service.atualizar(id, req);
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
