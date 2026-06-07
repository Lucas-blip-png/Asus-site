package com.asus.platform.web;

import com.asus.platform.domain.PresencaSessao;
import com.asus.platform.domain.Sessao;
import com.asus.platform.repository.PresencaSessaoRepository;
import com.asus.platform.repository.SessaoRepository;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

/** Calendario de sessoes e presenca (plano, Seção 18). */
@RestController
@RequestMapping("/api")
public class SessaoController {

    private final SessaoRepository sessaoRepository;
    private final PresencaSessaoRepository presencaRepository;

    public SessaoController(SessaoRepository sessaoRepository, PresencaSessaoRepository presencaRepository) {
        this.sessaoRepository = sessaoRepository;
        this.presencaRepository = presencaRepository;
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
        return sessaoRepository.save(s);
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
}
