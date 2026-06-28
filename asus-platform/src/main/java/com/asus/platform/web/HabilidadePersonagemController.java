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
import com.asus.platform.web.dto.HabilidadeEscolhidaResponse;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
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
    public List<HabilidadeEscolhidaResponse> escolhidas(@PathVariable Long id) {
        Personagem p = carregar(id);
        Map<String, Habilidade> catalogo = habilidadeRepository.findByGameSystemId(p.getGameSystemId()).stream()
                .collect(Collectors.toMap(Habilidade::getCodigo, Function.identity(), (a, b) -> a));
        return escolhidasRepository.findByPersonagemId(id).stream()
                .map(hp -> {
                    Habilidade h = catalogo.get(hp.getHabilidadeCodigo());
                    return h == null ? null : HabilidadeEscolhidaResponse.de(h, hp);
                })
                .filter(java.util.Objects::nonNull)
                .toList();
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

    /** Edita (override por personagem) os campos da habilidade escolhida. Campos vazios voltam ao catalogo. */
    @PutMapping("/{codigo}")
    public HabilidadeEscolhidaResponse editar(@PathVariable Long id, @PathVariable String codigo,
                                              @RequestBody Map<String, Object> body) {
        Personagem p = carregar(id);
        HabilidadePersonagem hp = escolhidasRepository.findByPersonagemId(id).stream()
                .filter(e -> e.getHabilidadeCodigo().equals(codigo)).findFirst()
                .orElseThrow(() -> new NotFoundException("Habilidade '" + codigo + "' nao esta na ficha"));
        Habilidade base = habilidadeRepository.findByGameSystemId(p.getGameSystemId()).stream()
                .filter(x -> x.getCodigo().equals(codigo)).findFirst()
                .orElseThrow(() -> new NotFoundException("Habilidade '" + codigo + "' nao encontrada"));

        hp.setNomeCustom(textoOuNull(body.get("nome")));
        hp.setTipoCustom(textoOuNull(body.get("tipo")));
        hp.setCustoTipoCustom(textoOuNull(body.get("custoTipo")));
        hp.setEfeitoCustom(textoOuNull(body.get("efeito")));
        Object custo = body.get("custo");
        hp.setCustoCustom(custo == null || String.valueOf(custo).isBlank()
                ? null : (int) Double.parseDouble(String.valueOf(custo)));
        escolhidasRepository.save(hp);
        return HabilidadeEscolhidaResponse.de(base, hp);
    }

    private static String textoOuNull(Object v) {
        if (v == null) {
            return null;
        }
        String s = String.valueOf(v).trim();
        return s.isEmpty() ? null : s;
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
