package com.asus.platform.web;

import com.asus.platform.domain.Notificacao;
import com.asus.platform.repository.NotificacaoRepository;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

/** Notificacoes do usuario (plano, Seção 17). usuarioId via query ate o JWT (Fase 7). */
@RestController
@RequestMapping("/api")
public class NotificacaoController {

    private final NotificacaoRepository repository;

    public NotificacaoController(NotificacaoRepository repository) {
        this.repository = repository;
    }

    @GetMapping("/me/notificacoes")
    public List<Notificacao> minhas(@RequestParam Long usuarioId) {
        return repository.findByUsuarioIdOrderByCriadaEmDesc(usuarioId);
    }

    @GetMapping("/me/notificacoes/nao-lidas")
    public Map<String, Long> naoLidas(@RequestParam Long usuarioId) {
        return Map.of("total", repository.countByUsuarioIdAndLidaFalse(usuarioId));
    }

    @PostMapping("/notificacoes")
    @ResponseStatus(HttpStatus.CREATED)
    public Notificacao criar(@RequestBody Notificacao n) {
        n.setId(null);
        n.setLida(false);
        return repository.save(n);
    }

    @PostMapping("/notificacoes/{id}/lida")
    public Notificacao marcarLida(@PathVariable Long id) {
        Notificacao n = repository.findById(id)
                .orElseThrow(() -> new NotFoundException("Notificacao " + id + " nao encontrada"));
        n.setLida(true);
        return repository.save(n);
    }
}
