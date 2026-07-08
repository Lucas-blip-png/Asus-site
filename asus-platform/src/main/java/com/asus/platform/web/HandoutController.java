package com.asus.platform.web;

import com.asus.platform.domain.Handout;
import com.asus.platform.domain.Permissao;
import com.asus.platform.repository.HandoutRepository;
import com.asus.platform.security.UsuarioPrincipal;
import com.asus.platform.service.CampanhaService;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/** Handouts da campanha: o mestre entrega imagens/notas aos jogadores em tempo real. */
@RestController
@RequestMapping("/api")
public class HandoutController {

    private final HandoutRepository repository;
    private final CampanhaService campanhaService;
    private final SimpMessagingTemplate messaging;

    public HandoutController(HandoutRepository repository,
                             CampanhaService campanhaService,
                             SimpMessagingTemplate messaging) {
        this.repository = repository;
        this.campanhaService = campanhaService;
        this.messaging = messaging;
    }

    @GetMapping("/campanhas/{id}/handouts")
    public List<Handout> listar(@PathVariable Long id) {
        return repository.findByCampanhaIdOrderByCriadoEmDescIdDesc(id);
    }

    @PostMapping("/campanhas/{id}/handouts")
    @ResponseStatus(HttpStatus.CREATED)
    public Handout criar(@PathVariable Long id, @RequestBody Map<String, Object> body,
                         @AuthenticationPrincipal UsuarioPrincipal principal) {
        if (principal != null) {
            campanhaService.exigirPermissao(id, principal.id(), Permissao.GERENCIAR_CAMPANHA);
        }
        String titulo = body.get("titulo") == null ? "" : String.valueOf(body.get("titulo")).trim();
        if (titulo.isEmpty()) {
            throw new IllegalArgumentException("titulo do handout e obrigatorio");
        }
        Long assetId = body.get("assetId") == null ? null
                : (long) Double.parseDouble(String.valueOf(body.get("assetId")));
        String texto = body.get("texto") == null ? null : String.valueOf(body.get("texto"));
        Handout h = repository.save(Handout.builder()
                .campanhaId(id).titulo(titulo).assetId(assetId).texto(texto).build());
        // Entrega em tempo real: os jogadores com a campanha aberta recebem na hora.
        messaging.convertAndSend("/topic/campanhas/" + id + "/handouts", h);
        return h;
    }

    @DeleteMapping("/handouts/{handoutId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void apagar(@PathVariable Long handoutId,
                       @AuthenticationPrincipal UsuarioPrincipal principal) {
        repository.findById(handoutId).ifPresent((h) -> {
            if (principal != null) {
                campanhaService.exigirPermissao(h.getCampanhaId(), principal.id(), Permissao.GERENCIAR_CAMPANHA);
            }
            repository.delete(h);
        });
    }
}
