package com.asus.platform.web;

import com.asus.platform.domain.Assinatura;
import com.asus.platform.service.AssinaturaService;
import com.asus.platform.service.PlanoService;
import com.asus.platform.web.dto.AssinaturaResponse;
import com.asus.platform.web.dto.DefinirPlanoRequest;
import com.asus.platform.web.dto.LimitesResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

/** Planos, assinatura manual e limites (plano, Seção 4 / Fase 10). */
@RestController
@RequestMapping("/api/organizacoes/{orgId}")
public class AssinaturaController {

    private final AssinaturaService assinaturaService;
    private final PlanoService planoService;

    public AssinaturaController(AssinaturaService assinaturaService, PlanoService planoService) {
        this.assinaturaService = assinaturaService;
        this.planoService = planoService;
    }

    @GetMapping("/assinatura")
    public AssinaturaResponse assinatura(@PathVariable Long orgId) {
        Assinatura a = assinaturaService.atual(orgId);
        return a != null ? AssinaturaResponse.de(a)
                : AssinaturaResponse.padrao(orgId, planoService.planoDa(orgId));
    }

    @PutMapping("/assinatura")
    public AssinaturaResponse definirPlano(@PathVariable Long orgId,
                                           @Valid @RequestBody DefinirPlanoRequest req) {
        return AssinaturaResponse.de(assinaturaService.definirPlano(orgId, req.plano()));
    }

    @GetMapping("/limites")
    public LimitesResponse limites(@PathVariable Long orgId) {
        return LimitesResponse.de(planoService.limitesDa(orgId));
    }
}
