package com.asus.platform.web;

import com.asus.platform.domain.AnalyticsEvent;
import com.asus.platform.repository.AnalyticsEventRepository;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

/** Analytics de eventos (plano, Seção 16). */
@RestController
@RequestMapping("/api")
public class AnalyticsController {

    private final AnalyticsEventRepository repository;

    public AnalyticsController(AnalyticsEventRepository repository) {
        this.repository = repository;
    }

    @PostMapping("/analytics")
    @ResponseStatus(HttpStatus.CREATED)
    public AnalyticsEvent registrar(@RequestBody AnalyticsEvent evento) {
        if (evento.getEvento() == null || evento.getEvento().isBlank()) {
            throw new IllegalArgumentException("evento e obrigatorio");
        }
        evento.setId(null);
        return repository.save(evento);
    }

    @GetMapping("/organizacoes/{orgId}/analytics")
    public List<AnalyticsEvent> porOrganizacao(@PathVariable Long orgId) {
        return repository.findByOrganizacaoIdOrderByCriadoEmDesc(orgId);
    }
}
