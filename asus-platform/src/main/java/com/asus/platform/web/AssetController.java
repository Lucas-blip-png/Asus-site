package com.asus.platform.web;

import com.asus.platform.service.AssetService;
import com.asus.platform.web.dto.AssetResponse;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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
                               @RequestParam(required = false) Long usuarioId) {
        return service.upload(orgId, tipo, publico, usuarioId, file);
    }

    @GetMapping("/organizacoes/{orgId}/assets")
    public List<AssetResponse> listar(@PathVariable Long orgId) {
        return service.listar(orgId);
    }

    @GetMapping("/assets/{id}/conteudo")
    public ResponseEntity<byte[]> conteudo(@PathVariable Long id) {
        AssetService.Conteudo c = service.baixar(id);
        MediaType tipo = c.mimeType() == null
                ? MediaType.APPLICATION_OCTET_STREAM
                : MediaType.parseMediaType(c.mimeType());
        return ResponseEntity.ok()
                .contentType(tipo)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + c.nomeOriginal() + "\"")
                .body(c.dados());
    }

    @DeleteMapping("/assets/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void apagar(@PathVariable Long id) {
        service.apagar(id);
    }
}
