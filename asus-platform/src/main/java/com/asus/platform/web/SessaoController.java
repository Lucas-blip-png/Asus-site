package com.asus.platform.web;

import com.asus.platform.domain.CampanhaMembro;
import com.asus.platform.domain.Notificacao;
import com.asus.platform.domain.Permissao;
import com.asus.platform.domain.PresencaSessao;
import com.asus.platform.domain.Sessao;
import com.asus.platform.repository.CampanhaMembroRepository;
import com.asus.platform.repository.NotificacaoRepository;
import com.asus.platform.repository.PresencaSessaoRepository;
import com.asus.platform.repository.SessaoRepository;
import com.asus.platform.security.UsuarioPrincipal;
import com.asus.platform.service.CampanhaService;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

/** Calendario de sessoes e presenca (plano, Seção 18). */
@RestController
@RequestMapping("/api")
public class SessaoController {

    private final SessaoRepository sessaoRepository;
    private final PresencaSessaoRepository presencaRepository;
    private final CampanhaMembroRepository membroRepository;
    private final NotificacaoRepository notificacaoRepository;
    private final CampanhaService campanhaService;

    public SessaoController(SessaoRepository sessaoRepository,
                            PresencaSessaoRepository presencaRepository,
                            CampanhaMembroRepository membroRepository,
                            NotificacaoRepository notificacaoRepository,
                            CampanhaService campanhaService) {
        this.sessaoRepository = sessaoRepository;
        this.presencaRepository = presencaRepository;
        this.membroRepository = membroRepository;
        this.notificacaoRepository = notificacaoRepository;
        this.campanhaService = campanhaService;
    }

    /** Gerenciar sessoes (criar/apagar/notificar) exige o mestre — quando ha usuario autenticado. */
    private void exigirMestre(Long campanhaId, UsuarioPrincipal principal) {
        if (principal != null) {
            campanhaService.exigirPermissao(campanhaId, principal.id(), Permissao.GERENCIAR_CAMPANHA);
        }
    }

    @GetMapping("/campanhas/{id}/sessoes")
    public List<Sessao> sessoes(@PathVariable Long id) {
        return sessaoRepository.findByCampanhaIdOrderByInicio(id);
    }

    @PostMapping("/campanhas/{id}/sessoes")
    @ResponseStatus(HttpStatus.CREATED)
    public Sessao criar(@PathVariable Long id, @RequestBody Sessao s,
                        @AuthenticationPrincipal UsuarioPrincipal principal) {
        exigirMestre(id, principal);
        if (s.getTitulo() == null || s.getTitulo().isBlank()) {
            throw new IllegalArgumentException("titulo da sessao e obrigatorio");
        }
        s.setId(null);
        s.setCampanhaId(id);
        if (s.getStatus() == null) {
            s.setStatus("AGENDADA");
        }
        Sessao salva = sessaoRepository.save(s);
        // Avisa todos os membros da campanha que uma sessao foi agendada.
        notificarMembros(id, "Nova sessão agendada",
                "A sessão \"" + salva.getTitulo() + "\" foi agendada. Confirme sua presença.");
        return salva;
    }

    @DeleteMapping("/sessoes/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Transactional
    public void apagar(@PathVariable Long id,
                       @AuthenticationPrincipal UsuarioPrincipal principal) {
        Sessao s = sessaoRepository.findById(id).orElse(null);
        if (s == null) {
            return;
        }
        exigirMestre(s.getCampanhaId(), principal);
        presencaRepository.deleteAll(presencaRepository.findBySessaoId(id));
        sessaoRepository.deleteById(id);
    }

    @GetMapping("/sessoes/{id}/presencas")
    public List<PresencaSessao> presencas(@PathVariable Long id) {
        return presencaRepository.findBySessaoId(id);
    }

    @PostMapping("/sessoes/{id}/presenca")
    public PresencaSessao confirmar(@PathVariable Long id, @RequestBody PresencaSessao req,
                                    @AuthenticationPrincipal UsuarioPrincipal principal) {
        // A presenca e sempre do usuario autenticado (nao aceita marcar pelos outros).
        Long usuarioId = principal != null ? principal.id() : req.getUsuarioId();
        PresencaSessao p = presencaRepository.findBySessaoIdAndUsuarioId(id, usuarioId)
                .orElseGet(() -> PresencaSessao.builder().sessaoId(id).usuarioId(usuarioId).build());
        p.setStatus(req.getStatus());
        return presencaRepository.save(p);
    }

    /** Notifica quem confirmou presenca (lembrete da sessao). So o mestre. */
    @PostMapping("/sessoes/{id}/notificar")
    public void notificar(@PathVariable Long id,
                          @AuthenticationPrincipal UsuarioPrincipal principal) {
        Sessao s = sessaoRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Sessao " + id + " nao encontrada"));
        exigirMestre(s.getCampanhaId(), principal);
        for (PresencaSessao p : presencaRepository.findBySessaoId(id)) {
            if ("CONFIRMADO".equalsIgnoreCase(p.getStatus())) {
                salvarNotificacao(p.getUsuarioId(), "Lembrete de sessão",
                        "A sessão \"" + s.getTitulo() + "\" está marcada. Até lá!");
            }
        }
    }

    // ----- helpers -----
    private void notificarMembros(Long campanhaId, String titulo, String mensagem) {
        for (CampanhaMembro m : membroRepository.findByCampanhaId(campanhaId)) {
            salvarNotificacao(m.getUsuarioId(), titulo, mensagem);
        }
    }

    private void salvarNotificacao(Long usuarioId, String titulo, String mensagem) {
        if (usuarioId == null) {
            return;
        }
        notificacaoRepository.save(Notificacao.builder()
                .usuarioId(usuarioId)
                .titulo(titulo)
                .mensagem(mensagem)
                .tipo("SESSAO")
                .lida(false)
                .criadaEm(LocalDateTime.now())
                .build());
    }
}
