package com.asus.platform.web;

import com.asus.platform.domain.CampanhaMembro;
import com.asus.platform.domain.Notificacao;
import com.asus.platform.domain.PresencaSessao;
import com.asus.platform.domain.Sessao;
import com.asus.platform.repository.CampanhaMembroRepository;
import com.asus.platform.repository.NotificacaoRepository;
import com.asus.platform.repository.PresencaSessaoRepository;
import com.asus.platform.repository.SessaoRepository;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.http.HttpStatus;
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

    public SessaoController(SessaoRepository sessaoRepository,
                            PresencaSessaoRepository presencaRepository,
                            CampanhaMembroRepository membroRepository,
                            NotificacaoRepository notificacaoRepository) {
        this.sessaoRepository = sessaoRepository;
        this.presencaRepository = presencaRepository;
        this.membroRepository = membroRepository;
        this.notificacaoRepository = notificacaoRepository;
    }

    @GetMapping("/campanhas/{id}/sessoes")
    public List<Sessao> sessoes(@PathVariable Long id) {
        return sessaoRepository.findByCampanhaIdOrderByInicio(id);
    }

    @PostMapping("/campanhas/{id}/sessoes")
    @ResponseStatus(HttpStatus.CREATED)
    public Sessao criar(@PathVariable Long id, @RequestBody Sessao s) {
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
    public void apagar(@PathVariable Long id) {
        presencaRepository.deleteAll(presencaRepository.findBySessaoId(id));
        sessaoRepository.deleteById(id);
    }

    @GetMapping("/sessoes/{id}/presencas")
    public List<PresencaSessao> presencas(@PathVariable Long id) {
        return presencaRepository.findBySessaoId(id);
    }

    @PostMapping("/sessoes/{id}/presenca")
    public PresencaSessao confirmar(@PathVariable Long id, @RequestBody PresencaSessao req) {
        PresencaSessao p = presencaRepository.findBySessaoIdAndUsuarioId(id, req.getUsuarioId())
                .orElseGet(() -> PresencaSessao.builder().sessaoId(id).usuarioId(req.getUsuarioId()).build());
        p.setStatus(req.getStatus());
        return presencaRepository.save(p);
    }

    /** Notifica quem confirmou presenca (lembrete da sessao). */
    @PostMapping("/sessoes/{id}/notificar")
    public void notificar(@PathVariable Long id) {
        Sessao s = sessaoRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Sessao " + id + " nao encontrada"));
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
