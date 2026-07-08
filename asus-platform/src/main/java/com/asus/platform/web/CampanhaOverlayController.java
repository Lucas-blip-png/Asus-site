package com.asus.platform.web;

import com.asus.platform.domain.Campanha;
import com.asus.platform.domain.Combate;
import com.asus.platform.domain.Personagem;
import com.asus.platform.domain.Rolagem;
import com.asus.platform.repository.CampanhaPersonagemRepository;
import com.asus.platform.repository.CampanhaRepository;
import com.asus.platform.repository.CombateParticipanteRepository;
import com.asus.platform.repository.CombateRepository;
import com.asus.platform.repository.PersonagemRepository;
import com.asus.platform.repository.RolagemRepository;
import com.asus.platform.web.dto.StatusDto;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Dados públicos da campanha para o overlay OBS da MESA inteira (retratos + barras
 * de todo o grupo + iniciativa do combate ativo) e para os "momentos da sessão"
 * (críticos e falhas), usados no recap.
 */
@RestController
@RequestMapping("/api/campanhas/{id}")
public class CampanhaOverlayController {

    private final CampanhaRepository campanhaRepository;
    private final CampanhaPersonagemRepository campanhaPersonagemRepository;
    private final PersonagemRepository personagemRepository;
    private final CombateRepository combateRepository;
    private final CombateParticipanteRepository participanteRepository;
    private final RolagemRepository rolagemRepository;

    public CampanhaOverlayController(CampanhaRepository campanhaRepository,
                                     CampanhaPersonagemRepository campanhaPersonagemRepository,
                                     PersonagemRepository personagemRepository,
                                     CombateRepository combateRepository,
                                     CombateParticipanteRepository participanteRepository,
                                     RolagemRepository rolagemRepository) {
        this.campanhaRepository = campanhaRepository;
        this.campanhaPersonagemRepository = campanhaPersonagemRepository;
        this.personagemRepository = personagemRepository;
        this.combateRepository = combateRepository;
        this.participanteRepository = participanteRepository;
        this.rolagemRepository = rolagemRepository;
    }

    /** Info mínima e pública para o overlay da mesa (sem dados sensíveis). */
    @GetMapping("/overlay")
    public Map<String, Object> overlay(@PathVariable Long id) {
        Campanha campanha = campanhaRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Campanha " + id + " nao encontrada"));

        List<Map<String, Object>> personagens = new ArrayList<>();
        campanhaPersonagemRepository.findByCampanhaId(id).forEach(cp ->
                personagemRepository.findById(cp.getPersonagemId()).ifPresent(p -> {
                    Map<String, Object> m = new HashMap<>();
                    m.put("id", p.getId());
                    m.put("nome", p.getNome());
                    m.put("avatarAssetId", p.getAvatarAssetId());
                    m.put("status", StatusDto.de(p.getStatus()));
                    personagens.add(m);
                }));

        // Combate ativo mais recente (se houver): ordem de iniciativa pro overlay.
        Map<String, Object> combate = combateRepository.findByCampanhaIdOrderByCriadoEmDesc(id).stream()
                .filter(Combate::isAtivo)
                .findFirst()
                .map(c -> {
                    List<Map<String, Object>> parts = new ArrayList<>();
                    participanteRepository.findByCombateIdOrderByIniciativaDescIdAsc(c.getId()).forEach(pp -> {
                        Map<String, Object> m = new HashMap<>();
                        m.put("nome", pp.getNome());
                        m.put("iniciativa", pp.getIniciativa());
                        m.put("inimigo", pp.isInimigo());
                        m.put("avatarAssetId", pp.getAvatarAssetId());
                        m.put("pvAtual", pp.getPvAtual());
                        m.put("pvMax", pp.getPvMax());
                        parts.add(m);
                    });
                    Map<String, Object> m = new HashMap<>();
                    m.put("id", c.getId());
                    m.put("nome", c.getNome());
                    m.put("rodada", c.getRodada());
                    m.put("turnoAtual", c.getTurnoAtual());
                    m.put("participantes", parts);
                    return m;
                })
                .orElse(null);

        Map<String, Object> resp = new HashMap<>();
        resp.put("nome", campanha.getNome());
        resp.put("personagens", personagens);
        resp.put("combate", combate);
        return resp;
    }

    /** Momentos da sessão: críticos e falhas críticas públicos (base do recap). */
    @GetMapping("/momentos")
    public List<Map<String, Object>> momentos(@PathVariable Long id) {
        campanhaRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Campanha " + id + " nao encontrada"));
        List<Map<String, Object>> momentos = new ArrayList<>();
        for (Rolagem r : rolagemRepository.findByCampanhaIdOrderByCriadoEmDescIdDesc(id)) {
            if (!r.isCritico() && !r.isFalhaCritica()) {
                continue;
            }
            if (r.isOculta() && !r.isRevelada()) {
                continue; // rolagem secreta do mestre não vaza no recap
            }
            Map<String, Object> m = new HashMap<>();
            m.put("tipo", r.isCritico() ? "CRITICO" : "FALHA");
            m.put("rotulo", r.getRotulo() == null || r.getRotulo().isBlank() ? r.getExpressao() : r.getRotulo());
            m.put("total", r.getTotal());
            m.put("criadoEm", r.getCriadoEm());
            m.put("personagemNome", r.getPersonagemId() == null ? null
                    : personagemRepository.findById(r.getPersonagemId()).map(Personagem::getNome).orElse(null));
            momentos.add(m);
        }
        return momentos;
    }
}
