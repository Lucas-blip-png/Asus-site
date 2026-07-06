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
import com.asus.platform.web.dto.HabilidadeDisponivelResponse;
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
    /** Codigo-dono sentinela das habilidades proprias (custom): nunca aparece em "disponiveis". */
    private static final String CODIGO_PROPRIA = "PROPRIA";

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
    public List<HabilidadeDisponivelResponse> disponiveis(@PathVariable Long id) {
        Personagem p = carregar(id);
        Set<String> classes = classesDoPersonagem(p);
        Set<String> cods = codigosEscolhidos(id);
        // Mostra todas as habilidades da(s) classe(s) do personagem (ou GERAL), inclusive as que
        // ainda estao bloqueadas por requisito — o front exibe o motivo e nao deixa pegar ate atingir.
        return habilidadeRepository.findByGameSystemId(p.getGameSystemId()).stream()
                .filter(h -> !cods.contains(h.getCodigo()))
                .filter(h -> pertenceClasse(h, classes))
                .map(h -> {
                    String motivo = motivoBloqueio(h, p);
                    return HabilidadeDisponivelResponse.de(h, motivo != null, motivo);
                })
                .sorted(java.util.Comparator.comparing(HabilidadeDisponivelResponse::bloqueada)
                        .thenComparing(HabilidadeDisponivelResponse::nome))
                .toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Habilidade adicionar(@PathVariable Long id, @RequestBody Map<String, String> body) {
        String codigo = body.get("codigo");
        Personagem p = carregar(id);
        Habilidade h = habilidadeRepository.findByGameSystemId(p.getGameSystemId()).stream()
                .filter(x -> x.getCodigo().equals(codigo)).findFirst()
                .orElseThrow(() -> new NotFoundException("Habilidade '" + codigo + "' nao encontrada"));
        if (!disponivel(h, p, classesDoPersonagem(p))) {
            throw new IllegalArgumentException("Pre-requisitos nao atendidos para " + h.getNome());
        }
        if (!escolhidasRepository.existsByPersonagemIdAndHabilidadeCodigo(id, codigo)) {
            escolhidasRepository.save(HabilidadePersonagem.builder()
                    .personagemId(id).habilidadeCodigo(codigo).build());
        }
        return h;
    }

    /**
     * Cria uma habilidade propria (custom) do personagem: entra no catalogo como nao-oficial,
     * com um codigo sentinela ("PROPRIA") que nunca aparece na lista de disponiveis de ninguem,
     * e ja fica escolhida na ficha deste personagem.
     */
    @PostMapping("/custom")
    @ResponseStatus(HttpStatus.CREATED)
    public HabilidadeEscolhidaResponse criarPropria(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        Personagem p = carregar(id);
        String nome = textoOuNull(body.get("nome"));
        if (nome == null) {
            throw new IllegalArgumentException("nome da habilidade e obrigatorio");
        }
        String tipo = textoOuNull(body.get("tipo"));
        String codigo = "PROPRIA_" + id + "_" + System.currentTimeMillis();
        Habilidade h = habilidadeRepository.save(Habilidade.builder()
                .gameSystemId(p.getGameSystemId())
                .codigo(codigo)
                .nome(nome)
                .classeCodigo(CODIGO_PROPRIA)
                .tipo(tipo == null ? "PASSIVA" : tipo)
                .custo(inteiro(body.get("custo")))
                .custoTipo(textoOuNull(body.get("custoTipo")))
                .efeito(textoOuNull(body.get("efeito")))
                .oficial(false)
                .build());
        HabilidadePersonagem hp = escolhidasRepository.save(HabilidadePersonagem.builder()
                .personagemId(id).habilidadeCodigo(codigo).build());
        return HabilidadeEscolhidaResponse.de(h, hp);
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

    private static int inteiro(Object v) {
        if (v == null) {
            return 0;
        }
        String s = String.valueOf(v).trim();
        if (s.isEmpty()) {
            return 0;
        }
        try {
            return (int) Double.parseDouble(s);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    @DeleteMapping("/{codigo}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void remover(@PathVariable Long id, @PathVariable String codigo) {
        List<HabilidadePersonagem> alvo = escolhidasRepository.findByPersonagemId(id).stream()
                .filter(e -> e.getHabilidadeCodigo().equals(codigo)).toList();
        escolhidasRepository.deleteAll(alvo);
        // Habilidade propria (custom): remove tambem a entrada orfa do catalogo.
        if (codigo != null && codigo.startsWith("PROPRIA_")) {
            habilidadeRepository.findByGameSystemId(carregar(id).getGameSystemId()).stream()
                    .filter(h -> codigo.equals(h.getCodigo()) && CODIGO_PROPRIA.equals(h.getClasseCodigo()))
                    .forEach(habilidadeRepository::delete);
        }
    }

    // ----- helpers -----

    private boolean disponivel(Habilidade h, Personagem p, Set<String> classesDoPersonagem) {
        return pertenceClasse(h, classesDoPersonagem) && motivoBloqueio(h, p) == null;
    }

    /** A habilidade e da(s) classe/trilha do personagem, ou GERAL (vale para todas). */
    private boolean pertenceClasse(Habilidade h, Set<String> classesDoPersonagem) {
        String donoRaw = h.getClasseCodigo() == null ? "" : h.getClasseCodigo();
        Set<String> donos = new HashSet<>();
        for (String c : donoRaw.split(",")) {
            String t = c.trim();
            if (!t.isBlank()) {
                donos.add(t);
            }
        }
        boolean geral = donos.isEmpty() || donos.contains("GERAL");
        return geral || donos.stream().anyMatch(classesDoPersonagem::contains);
    }

    /** Motivo do bloqueio por requisito (nivel/atributo), ou null se ja atende. */
    private String motivoBloqueio(Habilidade h, Personagem p) {
        int nivelMin = Math.max(1, h.getNivelMinimo());
        if (p.getNivel() < nivelMin) {
            return "Requer nível " + nivelMin;
        }
        if (h.getAtributoRequisito() != null && !h.getAtributoRequisito().isBlank()) {
            int val = p.getAtributosFinais() == null ? 0
                    : p.getAtributosFinais().get(Atributo.valueOf(h.getAtributoRequisito()));
            if (val < h.getValorAtributoRequisito()) {
                String n = h.getAtributoRequisito();
                return "Requer " + h.getValorAtributoRequisito() + " de "
                        + n.charAt(0) + n.substring(1).toLowerCase();
            }
        }
        return null;
    }

    /** Codigos das classes do personagem: primaria, trilha, secundaria e trilha secundaria. */
    private Set<String> classesDoPersonagem(Personagem p) {
        Set<String> s = new HashSet<>();
        for (Long cid : new Long[]{p.getClasseId(), p.getTrilhaId(),
                p.getClasseSecundariaId(), p.getTrilhaSecundariaId()}) {
            String c = codigo(cid);
            if (c != null) {
                s.add(c);
            }
        }
        return s;
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
