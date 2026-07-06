package com.asus.platform.service;

import com.asus.platform.domain.Atributo;
import com.asus.platform.domain.Classe;
import com.asus.platform.domain.Habilidade;
import com.asus.platform.domain.HabilidadePersonagem;
import com.asus.platform.domain.Personagem;
import com.asus.platform.repository.ClasseRepository;
import com.asus.platform.repository.HabilidadePersonagemRepository;
import com.asus.platform.repository.HabilidadeRepository;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Concede automaticamente as passivas das classes/trilhas do personagem.
 *
 * <p>No livro, as passivas da classe/raça são inerentes: o personagem já nasce com elas
 * e ganha as de nível maior conforme sobe de nível. Aqui: toda habilidade PASSIVA da
 * classe (não GERAL — essas são escolha) cujo requisito de nível/atributo já é atendido
 * entra sozinha na ficha. Chamado na criação e a cada level up.</p>
 */
@Service
public class PassivasService {

    private final HabilidadeRepository habilidadeRepository;
    private final HabilidadePersonagemRepository escolhidasRepository;
    private final ClasseRepository classeRepository;

    public PassivasService(HabilidadeRepository habilidadeRepository,
                           HabilidadePersonagemRepository escolhidasRepository,
                           ClasseRepository classeRepository) {
        this.habilidadeRepository = habilidadeRepository;
        this.escolhidasRepository = escolhidasRepository;
        this.classeRepository = classeRepository;
    }

    @Transactional
    public void concederPassivas(Personagem p) {
        if (p == null || p.getId() == null) {
            return;
        }
        Set<String> classes = classesDoPersonagem(p);
        if (classes.isEmpty()) {
            return;
        }
        Set<String> jaTem = escolhidasRepository.findByPersonagemId(p.getId()).stream()
                .map(HabilidadePersonagem::getHabilidadeCodigo).collect(Collectors.toSet());

        for (Habilidade h : habilidadeRepository.findByGameSystemId(p.getGameSystemId())) {
            if (!"PASSIVA".equalsIgnoreCase(h.getTipo())) {
                continue;
            }
            if (jaTem.contains(h.getCodigo())) {
                continue;
            }
            if (!ehDaClasse(h, classes)) {
                continue; // GERAL e de outras classes ficam de fora (GERAL é escolha)
            }
            if (!requisitosAtendidos(h, p)) {
                continue; // ainda não atingiu o nível/atributo — entra quando "upar"
            }
            escolhidasRepository.save(HabilidadePersonagem.builder()
                    .personagemId(p.getId()).habilidadeCodigo(h.getCodigo()).build());
            jaTem.add(h.getCodigo());
        }
    }

    /** A passiva é especificamente de uma das classes/trilhas do personagem (não GERAL). */
    private boolean ehDaClasse(Habilidade h, Set<String> classes) {
        String raw = h.getClasseCodigo() == null ? "" : h.getClasseCodigo();
        for (String c : raw.split(",")) {
            String t = c.trim();
            if (!t.isBlank() && !"GERAL".equals(t) && classes.contains(t)) {
                return true;
            }
        }
        return false;
    }

    private boolean requisitosAtendidos(Habilidade h, Personagem p) {
        if (p.getNivel() < Math.max(1, h.getNivelMinimo())) {
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

    private Set<String> classesDoPersonagem(Personagem p) {
        Set<String> s = new HashSet<>();
        for (Long cid : new Long[]{p.getClasseId(), p.getTrilhaId(),
                p.getClasseSecundariaId(), p.getTrilhaSecundariaId()}) {
            if (cid != null) {
                classeRepository.findById(cid).map(Classe::getCodigo).ifPresent(s::add);
            }
        }
        return s;
    }
}
