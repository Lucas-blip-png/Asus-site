package com.asus.platform.web;

import com.asus.platform.domain.Ataque;
import com.asus.platform.domain.FeiticoPersonagem;
import com.asus.platform.repository.AtaqueRepository;
import com.asus.platform.repository.FeiticoPersonagemRepository;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

/** Ataques e feiticos salvos na ficha (abas Combate e Magias). */
@RestController
@RequestMapping("/api")
public class ConteudoPersonagemController {

    private final AtaqueRepository ataqueRepository;
    private final FeiticoPersonagemRepository feiticoRepository;

    public ConteudoPersonagemController(AtaqueRepository ataqueRepository,
                                        FeiticoPersonagemRepository feiticoRepository) {
        this.ataqueRepository = ataqueRepository;
        this.feiticoRepository = feiticoRepository;
    }

    @GetMapping("/personagens/{id}/ataques")
    public List<Ataque> ataques(@PathVariable Long id) {
        return ataqueRepository.findByPersonagemId(id);
    }

    @PostMapping("/personagens/{id}/ataques")
    @ResponseStatus(HttpStatus.CREATED)
    public Ataque criarAtaque(@PathVariable Long id, @RequestBody Ataque a) {
        if (a.getNome() == null || a.getNome().isBlank()) {
            throw new IllegalArgumentException("nome do ataque e obrigatorio");
        }
        a.setId(null);
        a.setPersonagemId(id);
        return ataqueRepository.save(a);
    }

    @DeleteMapping("/ataques/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void apagarAtaque(@PathVariable Long id) {
        ataqueRepository.deleteById(id);
    }

    @GetMapping("/personagens/{id}/feiticos")
    public List<FeiticoPersonagem> feiticos(@PathVariable Long id) {
        return feiticoRepository.findByPersonagemId(id);
    }

    @PostMapping("/personagens/{id}/feiticos")
    @ResponseStatus(HttpStatus.CREATED)
    public FeiticoPersonagem criarFeitico(@PathVariable Long id, @RequestBody FeiticoPersonagem f) {
        if (f.getNome() == null || f.getNome().isBlank()) {
            throw new IllegalArgumentException("nome do feitico e obrigatorio");
        }
        f.setId(null);
        f.setPersonagemId(id);
        return feiticoRepository.save(f);
    }

    @DeleteMapping("/feiticos/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void apagarFeitico(@PathVariable Long id) {
        feiticoRepository.deleteById(id);
    }
}
