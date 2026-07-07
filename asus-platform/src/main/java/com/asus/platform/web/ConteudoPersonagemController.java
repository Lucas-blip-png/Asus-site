package com.asus.platform.web;

import com.asus.platform.domain.Ataque;
import com.asus.platform.domain.FeiticoPersonagem;
import com.asus.platform.repository.AtaqueRepository;
import com.asus.platform.repository.FeiticoPersonagemRepository;
import com.asus.platform.security.UsuarioPrincipal;
import com.asus.platform.service.AcessoService;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/** Ataques e feiticos salvos na ficha (abas Combate e Magias). */
@RestController
@RequestMapping("/api")
public class ConteudoPersonagemController {

    private final AtaqueRepository ataqueRepository;
    private final FeiticoPersonagemRepository feiticoRepository;
    private final AcessoService acessoService;

    public ConteudoPersonagemController(AtaqueRepository ataqueRepository,
                                        FeiticoPersonagemRepository feiticoRepository,
                                        AcessoService acessoService) {
        this.ataqueRepository = ataqueRepository;
        this.feiticoRepository = feiticoRepository;
        this.acessoService = acessoService;
    }

    @GetMapping("/personagens/{id}/ataques")
    public List<Ataque> ataques(@PathVariable Long id) {
        return ataqueRepository.findByPersonagemId(id);
    }

    @PostMapping("/personagens/{id}/ataques")
    @ResponseStatus(HttpStatus.CREATED)
    public Ataque criarAtaque(@PathVariable Long id, @RequestBody Ataque a,
                              @AuthenticationPrincipal UsuarioPrincipal principal) {
        acessoService.exigirDonoPersonagem(id, principal);
        if (a.getNome() == null || a.getNome().isBlank()) {
            throw new IllegalArgumentException("nome do ataque e obrigatorio");
        }
        a.setId(null);
        a.setPersonagemId(id);
        return ataqueRepository.save(a);
    }

    @PutMapping("/ataques/{id}")
    public Ataque atualizarAtaque(@PathVariable Long id, @RequestBody Ataque dados,
                                  @AuthenticationPrincipal UsuarioPrincipal principal) {
        Ataque a = ataqueRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("ataque nao encontrado"));
        acessoService.exigirDonoPersonagem(a.getPersonagemId(), principal);
        if (dados.getNome() != null && !dados.getNome().isBlank()) {
            a.setNome(dados.getNome());
        }
        a.setDano(dados.getDano());
        a.setCritico(dados.getCritico());
        a.setAlcance(dados.getAlcance());
        a.setPericia(dados.getPericia());
        a.setEfeito(dados.getEfeito());
        return ataqueRepository.save(a);
    }

    @DeleteMapping("/ataques/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void apagarAtaque(@PathVariable Long id,
                             @AuthenticationPrincipal UsuarioPrincipal principal) {
        Ataque a = ataqueRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("ataque nao encontrado"));
        acessoService.exigirDonoPersonagem(a.getPersonagemId(), principal);
        ataqueRepository.delete(a);
    }

    @GetMapping("/personagens/{id}/feiticos")
    public List<FeiticoPersonagem> feiticos(@PathVariable Long id) {
        return feiticoRepository.findByPersonagemId(id);
    }

    @PostMapping("/personagens/{id}/feiticos")
    @ResponseStatus(HttpStatus.CREATED)
    public FeiticoPersonagem criarFeitico(@PathVariable Long id, @RequestBody FeiticoPersonagem f,
                                          @AuthenticationPrincipal UsuarioPrincipal principal) {
        acessoService.exigirDonoPersonagem(id, principal);
        if (f.getNome() == null || f.getNome().isBlank()) {
            throw new IllegalArgumentException("nome do feitico e obrigatorio");
        }
        f.setId(null);
        f.setPersonagemId(id);
        return feiticoRepository.save(f);
    }

    @PutMapping("/feiticos/{id}")
    public FeiticoPersonagem atualizarFeitico(@PathVariable Long id, @RequestBody FeiticoPersonagem dados,
                                              @AuthenticationPrincipal UsuarioPrincipal principal) {
        FeiticoPersonagem f = feiticoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("feitico nao encontrado"));
        acessoService.exigirDonoPersonagem(f.getPersonagemId(), principal);
        if (dados.getNome() != null && !dados.getNome().isBlank()) {
            f.setNome(dados.getNome());
        }
        f.setCirculo(dados.getCirculo());
        f.setCustoPm(dados.getCustoPm());
        f.setAlcance(dados.getAlcance());
        f.setDuracao(dados.getDuracao());
        f.setEfeito(dados.getEfeito());
        return feiticoRepository.save(f);
    }

    @DeleteMapping("/feiticos/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void apagarFeitico(@PathVariable Long id,
                              @AuthenticationPrincipal UsuarioPrincipal principal) {
        feiticoRepository.findById(id).ifPresent((f) -> {
            acessoService.exigirDonoPersonagem(f.getPersonagemId(), principal);
            feiticoRepository.delete(f);
        });
    }
}
