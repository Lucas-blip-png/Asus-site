package com.asus.platform.web;

import com.asus.platform.domain.Cena;
import com.asus.platform.domain.Permissao;
import com.asus.platform.repository.CenaRepository;
import com.asus.platform.security.UsuarioPrincipal;
import com.asus.platform.service.CampanhaService;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/** Cenas/seções com mapa 2D (fundo + névoa + tokens). O mestre monta; jogadores veem a cena ativa. */
@RestController
@RequestMapping("/api")
public class CenaController {

    private final CenaRepository repository;
    private final CampanhaService campanhaService;
    private final SimpMessagingTemplate messaging;

    public CenaController(CenaRepository repository, CampanhaService campanhaService,
                          SimpMessagingTemplate messaging) {
        this.repository = repository;
        this.campanhaService = campanhaService;
        this.messaging = messaging;
    }

    @GetMapping("/campanhas/{id}/cenas")
    public List<Cena> listar(@PathVariable Long id) {
        return repository.findByCampanhaIdOrderByCriadoEmDescIdDesc(id);
    }

    @PostMapping("/campanhas/{id}/cenas")
    @ResponseStatus(HttpStatus.CREATED)
    public Cena criar(@PathVariable Long id, @RequestBody Map<String, Object> body,
                      @AuthenticationPrincipal UsuarioPrincipal principal) {
        exigirMestre(id, principal);
        String nome = body.get("nome") == null ? "" : String.valueOf(body.get("nome")).trim();
        if (nome.isEmpty()) {
            nome = "Nova cena";
        }
        Cena c = repository.save(Cena.builder().campanhaId(id).nome(nome).build());
        avisar(id);
        return c;
    }

    @PutMapping("/cenas/{cenaId}")
    public Cena atualizar(@PathVariable Long cenaId, @RequestBody Map<String, Object> body,
                          @AuthenticationPrincipal UsuarioPrincipal principal) {
        Cena c = repository.findById(cenaId)
                .orElseThrow(() -> new NotFoundException("Cena " + cenaId + " nao encontrada"));
        exigirMestre(c.getCampanhaId(), principal);
        if (body.containsKey("nome") && body.get("nome") != null) {
            String n = String.valueOf(body.get("nome")).trim();
            if (!n.isEmpty()) {
                c.setNome(n);
            }
        }
        if (body.containsKey("mapaAssetId")) {
            Object v = body.get("mapaAssetId");
            c.setMapaAssetId(v == null ? null : (long) Double.parseDouble(String.valueOf(v)));
        }
        if (body.containsKey("fogJson")) {
            Object v = body.get("fogJson");
            c.setFogJson(v == null ? null : String.valueOf(v));
        }
        if (body.containsKey("tokensJson")) {
            Object v = body.get("tokensJson");
            c.setTokensJson(v == null ? null : String.valueOf(v));
        }
        c = repository.save(c);
        avisar(c.getCampanhaId());
        return c;
    }

    /** Torna esta a cena ATIVA da campanha (desativa as demais). */
    @PostMapping("/cenas/{cenaId}/ativar")
    public Cena ativar(@PathVariable Long cenaId,
                       @AuthenticationPrincipal UsuarioPrincipal principal) {
        Cena alvo = repository.findById(cenaId)
                .orElseThrow(() -> new NotFoundException("Cena " + cenaId + " nao encontrada"));
        exigirMestre(alvo.getCampanhaId(), principal);
        for (Cena c : repository.findByCampanhaIdOrderByCriadoEmDescIdDesc(alvo.getCampanhaId())) {
            boolean deveAtivar = c.getId().equals(cenaId);
            if (c.isAtiva() != deveAtivar) {
                c.setAtiva(deveAtivar);
                repository.save(c);
            }
        }
        avisar(alvo.getCampanhaId());
        return repository.findById(cenaId).orElse(alvo);
    }

    @DeleteMapping("/cenas/{cenaId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void apagar(@PathVariable Long cenaId,
                       @AuthenticationPrincipal UsuarioPrincipal principal) {
        repository.findById(cenaId).ifPresent(c -> {
            exigirMestre(c.getCampanhaId(), principal);
            repository.delete(c);
            avisar(c.getCampanhaId());
        });
    }

    private void exigirMestre(Long campanhaId, UsuarioPrincipal principal) {
        if (principal != null) {
            campanhaService.exigirPermissao(campanhaId, principal.id(), Permissao.GERENCIAR_CAMPANHA);
        }
    }

    /** Notifica os jogadores que a cena mudou (recarregam a aba). */
    private void avisar(Long campanhaId) {
        messaging.convertAndSend("/topic/campanhas/" + campanhaId + "/cenas", Map.of("atualizado", true));
    }
}
