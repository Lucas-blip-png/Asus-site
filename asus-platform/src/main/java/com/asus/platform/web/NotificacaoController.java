package com.asus.platform.web;

import com.asus.platform.domain.Notificacao;
import com.asus.platform.repository.NotificacaoRepository;
import com.asus.platform.security.UsuarioPrincipal;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/** Notificacoes do usuario (plano, Seção 17). O dono vem do JWT; o usuarioId da query so vale no modo aberto. */
@RestController
@RequestMapping("/api")
public class NotificacaoController {

    private final NotificacaoRepository repository;

    public NotificacaoController(NotificacaoRepository repository) {
        this.repository = repository;
    }

    /** Id do usuario efetivo: o do JWT quando autenticado, senao o da query (modo aberto/dev). */
    private static Long uid(UsuarioPrincipal principal, Long usuarioIdQuery) {
        return principal != null ? principal.id() : usuarioIdQuery;
    }

    @GetMapping("/me/notificacoes")
    public List<Notificacao> minhas(@RequestParam(required = false) Long usuarioId,
                                    @AuthenticationPrincipal UsuarioPrincipal principal) {
        return repository.findByUsuarioIdOrderByCriadaEmDesc(uid(principal, usuarioId));
    }

    @GetMapping("/me/notificacoes/nao-lidas")
    public Map<String, Long> naoLidas(@RequestParam(required = false) Long usuarioId,
                                      @AuthenticationPrincipal UsuarioPrincipal principal) {
        return Map.of("total", repository.countByUsuarioIdAndLidaFalse(uid(principal, usuarioId)));
    }

    @PostMapping("/notificacoes")
    @ResponseStatus(HttpStatus.CREATED)
    public Notificacao criar(@RequestBody Notificacao n) {
        n.setId(null);
        n.setLida(false);
        return repository.save(n);
    }

    /** Marca todas as notificacoes nao lidas do usuario como lidas (zera o contador do sino). */
    @PostMapping("/me/notificacoes/marcar-todas-lidas")
    public Map<String, Long> marcarTodasLidas(@RequestParam(required = false) Long usuarioId,
                                              @AuthenticationPrincipal UsuarioPrincipal principal) {
        List<Notificacao> pendentes = repository.findByUsuarioIdAndLidaFalse(uid(principal, usuarioId));
        pendentes.forEach(n -> n.setLida(true));
        repository.saveAll(pendentes);
        return Map.of("marcadas", (long) pendentes.size());
    }

    @PostMapping("/notificacoes/{id}/lida")
    public Notificacao marcarLida(@PathVariable Long id,
                                  @AuthenticationPrincipal UsuarioPrincipal principal) {
        Notificacao n = repository.findById(id)
                .orElseThrow(() -> new NotFoundException("Notificacao " + id + " nao encontrada"));
        if (principal != null && !principal.id().equals(n.getUsuarioId())) {
            throw new AcessoNegadoException("Notificação de outro usuário.");
        }
        n.setLida(true);
        return repository.save(n);
    }
}
