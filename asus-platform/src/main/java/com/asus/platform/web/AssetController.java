package com.asus.platform.web;

import com.asus.platform.security.UsuarioPrincipal;
import com.asus.platform.service.AssetService;
import com.asus.platform.web.dto.AssetResponse;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/** Assets/midia (plano, Seção 13 e 21.6 / Fase 11). */
@RestController
@RequestMapping("/api")
public class AssetController {

    private final AssetService service;

    public AssetController(AssetService service) {
        this.service = service;
    }

    @PostMapping(value = "/organizacoes/{orgId}/assets", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public AssetResponse upload(@PathVariable Long orgId,
                               @RequestParam("file") MultipartFile file,
                               @RequestParam(defaultValue = "OUTRO") String tipo,
                               @RequestParam(defaultValue = "false") boolean publico,
                               @RequestParam(required = false) Long usuarioId,
                               @AuthenticationPrincipal UsuarioPrincipal principal) {
        // O dono do upload e o usuario autenticado (a query so vale no modo aberto/dev).
        Long uid = principal != null ? principal.id() : usuarioId;
        return service.upload(orgId, tipo, publico, uid, file);
    }

    @GetMapping("/organizacoes/{orgId}/assets")
    public List<AssetResponse> listar(@PathVariable Long orgId) {
        return service.listar(orgId);
    }

    @GetMapping("/assets/{id}/conteudo")
    public ResponseEntity<byte[]> conteudo(@PathVariable Long id,
                                           @AuthenticationPrincipal UsuarioPrincipal principal) {
        AssetService.Conteudo c = service.baixar(id);
        MediaType tipo = c.mimeType() == null
                ? MediaType.APPLICATION_OCTET_STREAM
                : MediaType.parseMediaType(c.mimeType());
        // A rota e publica para IMAGENS (avatares/capas carregam via <img>, sem header).
        // Conteudo que nao e imagem (PDF/texto/JSON) exige usuario autenticado.
        if (principal == null && !tipo.toString().startsWith("image/")) {
            throw new NotFoundException("Asset " + id + " nao disponivel publicamente");
        }
        return ResponseEntity.ok()
                .contentType(tipo)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + c.nomeOriginal() + "\"")
                .body(c.dados());
    }

    @DeleteMapping("/assets/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void apagar(@PathVariable Long id,
                       @AuthenticationPrincipal UsuarioPrincipal principal) {
        service.apagar(id, principal);
    }
}
