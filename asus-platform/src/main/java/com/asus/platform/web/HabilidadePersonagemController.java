package com.asus.platform.web;

import com.asus.platform.domain.Atributo;
import com.asus.platform.domain.Classe;
import com.asus.platform.domain.Habilidade;
import com.asus.platform.domain.HabilidadePersonagem;
import com.asus.platform.domain.Personagem;
import com.asus.platform.repository.ClasseRepository;
import com.asus.platform.repository.HabilidadePersonagemRepository;
import com.asus.platform.repository.HabilidadeRepository;
import com.asus.platform.repository.PersonagemRepository;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

/**
 * Habilidades escolhidas por um personagem, com pré-requisitos:
 * a habilidade precisa ser da classe/trilha do personagem (ou GERAL), o nível
 * deve alcançar o exigido (trilha só a partir do nível 11) e o atributo exigido
 * deve ser atendido. O total respeita o limite (Destreza / 2).
 */
@RestController
@RequestMapping("/api/personagens/{id}/habilidades")
public class HabilidadePersonagemController {

    private static final int NIVEL_TRILHA = 11;

    private final PersonagemRepository personagemRepository;
    private final ClasseRepository classeRepository;
    private final HabilidadeRepository habilidadeRepository;
    private final HabilidadePersonagemRepository escolhidasRepository;

    public HabilidadePersonagemController(PersonagemRepository personagemRepository,
                                          ClasseRepository classeRepository,
                                          HabilidadeRepository habilidadeRepository,
                                          HabilidadePersonagemRepository escolhidasRepository) {
        this.personagemRepository = personagemRepository;
        this.classeRepository = classeRepository;
        this.habilidadeRepository = habilidadeRepository;
        this.escolhidasRepository = escolhidasRepository;
    }

    @GetMapping
    public List<Habilidade> escolhidas(@PathVariable Long id) {
        Personagem p = carregar(id);
        Set<String> cods = codigosEscolhidos(id);
        return habilidadeRepository.findByGameSystemId(p.getGameSystemId()).stream()
                .filter(h -> cods.contains(h.getCodigo())).toList();
    }

    @GetMapping("/disponiveis")
    public List<Habilidade> disponiveis(@PathVariable Long id) {
        Personagem p = carregar(id);
        String classeCod = codigo(p.getClasseId());
        String trilhaCod = codigo(p.getTrilhaId());
        Set<String> cods = codigosEscolhidos(id);
        return habilidadeRepository.findByGameSystemId(p.getGameSystemId()).stream()
                .filter(h -> !cods.contains(h.getCodigo()))
                .filter(h -> disponivel(h, p, classeCod, trilhaCod))
                .toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Habilidade adicionar(@PathVariable Long id, @RequestBody Map<String, String> body) {
        String codigo = body.get("codigo");
        Personagem p = carregar(id);
        String classeCod = codigo(p.getClasseId());
        String trilhaCod = codigo(p.getTrilhaId());
        Habilidade h = habilidadeRepository.findByGameSystemId(p.getGameSystemId()).stream()
                .filter(x -> x.getCodigo().equals(codigo)).findFirst()
                .orElseThrow(() -> new NotFoundException("Habilidade '" + codigo + "' nao encontrada"));
        if (!disponivel(h, p, classeCod, trilhaCod)) {
            throw new IllegalArgumentException("Pre-requisitos nao atendidos para " + h.getNome());
        }
        int limite = (p.getAtributosFinais() == null ? 0 : p.getAtributosFinais().getDestreza()) / 2;
        if (codigosEscolhidos(id).size() >= limite) {
            throw new IllegalArgumentException("Limite de habilidades atingido (" + limite + ")");
        }
        if (!escolhidasRepository.existsByPersonagemIdAndHabilidadeCodigo(id, codigo)) {
            escolhidasRepository.save(HabilidadePersonagem.builder()
                    .personagemId(id).habilidadeCodigo(codigo).build());
        }
        return h;
    }

    @DeleteMapping("/{codigo}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void remover(@PathVariable Long id, @PathVariable String codigo) {
        List<HabilidadePersonagem> alvo = escolhidasRepository.findByPersonagemId(id).stream()
                .filter(e -> e.getHabilidadeCodigo().equals(codigo)).toList();
        escolhidasRepository.deleteAll(alvo);
    }

    // ----- helpers -----

    private boolean disponivel(Habilidade h, Personagem p, String classeCod, String trilhaCod) {
        String dono = h.getClasseCodigo();
        boolean daTrilha = trilhaCod != null && trilhaCod.equals(dono);
        boolean pertence = "GERAL".equals(dono) || (classeCod != null && classeCod.equals(dono)) || daTrilha;
        if (!pertence) {
            return false;
        }
        int nivelMin = Math.max(1, h.getNivelMinimo());
        if (daTrilha) {
            nivelMin = Math.max(nivelMin, NIVEL_TRILHA); // trilha so a partir do nivel 11
        }
        if (p.getNivel() < nivelMin) {
            return false;
        }
        if (h.getAtributoRequisito() != null && !h.getAtributoRequisito().isBlank()) {
            int val = p.getAtributosFinais() == null ? 0
                    : p.getAtributosFinais().get(Atributo.valueOf(h.getAtributoRequisito()));
            if (val < h.getValorAtributoRequisito()) {
                return false;
            }
        }
        return true;
    }

    private Set<String> codigosEscolhidos(Long id) {
        Set<String> s = new HashSet<>();
        escolhidasRepository.findByPersonagemId(id).forEach(e -> s.add(e.getHabilidadeCodigo()));
        return s;
    }

    private String codigo(Long classeId) {
        if (classeId == null) {
            return null;
        }
        return classeRepository.findById(classeId).map(Classe::getCodigo).orElse(null);
    }

    private Personagem carregar(Long id) {
        return personagemRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Personagem " + id + " nao encontrado"));
    }
}
