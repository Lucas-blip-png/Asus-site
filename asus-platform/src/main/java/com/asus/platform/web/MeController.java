package com.asus.platform.web;

import com.asus.platform.service.LgpdService;
import com.asus.platform.web.dto.ConsentimentoResponse;
import com.asus.platform.web.dto.RegistrarConsentimentoRequest;
import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

/**
 * Direitos do titular dos dados (plano, Seção 19.3 / Fase 13).
 *
 * <p>Sem login ainda (Fase 7): o usuario vem por {@code ?usuarioId=}. Na Fase 7
 * passa a vir do JWT e o parametro sai.</p>
 */
@RestController
@RequestMapping("/api/me")
public class MeController {

    private final LgpdService service;

    public MeController(LgpdService service) {
        this.service = service;
    }

    @GetMapping("/export-data")
    public Map<String, Object> exportData(@RequestParam Long usuarioId) {
        return service.exportarDados(usuarioId);
    }

    @PostMapping("/consentimentos")
    @ResponseStatus(HttpStatus.CREATED)
    public ConsentimentoResponse consentir(@RequestParam Long usuarioId,
                                           @Valid @RequestBody RegistrarConsentimentoRequest req) {
        return service.registrarConsentimento(usuarioId, req.tipo(), req.versaoDocumento(), req.aceito());
    }

    @DeleteMapping("/delete-account")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteAccount(@RequestParam Long usuarioId) {
        service.excluirConta(usuarioId);
    }
}
