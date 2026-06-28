package com.asus.platform.web;

import com.asus.platform.domain.Combate;
import com.asus.platform.domain.CombateParticipante;
import com.asus.platform.repository.CombateParticipanteRepository;
import com.asus.platform.repository.CombateRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

/** Combates (rastreador de iniciativa) de uma campanha. */
@RestController
@RequestMapping("/api")
public class CombateController {

    private final CombateRepository combateRepository;
    private final CombateParticipanteRepository participanteRepository;

    public CombateController(CombateRepository combateRepository,
                             CombateParticipanteRepository participanteRepository) {
        this.combateRepository = combateRepository;
        this.participanteRepository = participanteRepository;
    }

    @GetMapping("/campanhas/{campanhaId}/combates")
    public List<Combate> listar(@PathVariable Long campanhaId) {
        return combateRepository.findByCampanhaIdOrderByCriadoEmDesc(campanhaId);
    }

    @PostMapping("/campanhas/{campanhaId}/combates")
    @ResponseStatus(HttpStatus.CREATED)
    public Combate criar(@PathVariable Long campanhaId, @RequestBody Map<String, Object> body) {
        String nome = texto(body.get("nome"));
        if (nome == null) {
            nome = "Combate";
        }
        return combateRepository.save(Combate.builder()
                .campanhaId(campanhaId)
                .nome(nome)
                .rodada(1)
                .turnoAtual(0)
                .ativo(true)
                .criadoEm(LocalDateTime.now())
                .build());
    }

    @GetMapping("/combates/{id}")
    public Combate buscar(@PathVariable Long id) {
        return combateRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Combate " + id + " nao encontrado"));
    }

    @PutMapping("/combates/{id}")
    public Combate atualizar(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        Combate c = buscar(id);
        if (body.containsKey("nome") && texto(body.get("nome")) != null) {
            c.setNome(texto(body.get("nome")));
        }
        if (body.containsKey("ativo")) {
            c.setAtivo(Boolean.parseBoolean(String.valueOf(body.get("ativo"))));
        }
        return combateRepository.save(c);
    }

    @DeleteMapping("/combates/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Transactional
    public void apagar(@PathVariable Long id) {
        participanteRepository.deleteAll(participanteRepository.findByCombateIdOrderByIniciativaDescIdAsc(id));
        combateRepository.deleteById(id);
    }

    /** Avança o turno; ao passar do último participante, incrementa a rodada. */
    @PostMapping("/combates/{id}/proximo")
    public Combate proximoTurno(@PathVariable Long id) {
        Combate c = buscar(id);
        int total = participanteRepository.findByCombateIdOrderByIniciativaDescIdAsc(id).size();
        if (total == 0) {
            return c;
        }
        int prox = c.getTurnoAtual() + 1;
        if (prox >= total) {
            prox = 0;
            c.setRodada(c.getRodada() + 1);
        }
        c.setTurnoAtual(prox);
        return combateRepository.save(c);
    }

    @PostMapping("/combates/{id}/reset")
    public Combate reset(@PathVariable Long id) {
        Combate c = buscar(id);
        c.setRodada(1);
        c.setTurnoAtual(0);
        return combateRepository.save(c);
    }

    @GetMapping("/combates/{id}/participantes")
    public List<CombateParticipante> participantes(@PathVariable Long id) {
        return participanteRepository.findByCombateIdOrderByIniciativaDescIdAsc(id);
    }

    @PostMapping("/combates/{id}/participantes")
    @ResponseStatus(HttpStatus.CREATED)
    public CombateParticipante adicionar(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        String nome = texto(body.get("nome"));
        if (nome == null) {
            throw new IllegalArgumentException("nome do participante e obrigatorio");
        }
        int pvMax = inteiro(body.get("pvMax"), 0);
        int pvAtual = body.get("pvAtual") == null ? pvMax : inteiro(body.get("pvAtual"), pvMax);
        return participanteRepository.save(CombateParticipante.builder()
                .combateId(id)
                .personagemId(body.get("personagemId") == null ? null : longOuNull(body.get("personagemId")))
                .avatarAssetId(body.get("avatarAssetId") == null ? null : longOuNull(body.get("avatarAssetId")))
                .nome(nome)
                .iniciativa(inteiro(body.get("iniciativa"), 0))
                .pvMax(pvMax)
                .pvAtual(pvAtual)
                .inimigo(Boolean.parseBoolean(String.valueOf(body.get("inimigo"))))
                .build());
    }

    @PutMapping("/participantes/{pid}")
    public CombateParticipante atualizarParticipante(@PathVariable Long pid, @RequestBody Map<String, Object> body) {
        CombateParticipante p = participanteRepository.findById(pid)
                .orElseThrow(() -> new NotFoundException("Participante " + pid + " nao encontrado"));
        if (body.containsKey("nome") && texto(body.get("nome")) != null) {
            p.setNome(texto(body.get("nome")));
        }
        if (body.containsKey("iniciativa")) {
            p.setIniciativa(inteiro(body.get("iniciativa"), p.getIniciativa()));
        }
        if (body.containsKey("pvAtual")) {
            p.setPvAtual(inteiro(body.get("pvAtual"), p.getPvAtual()));
        }
        if (body.containsKey("pvMax")) {
            p.setPvMax(inteiro(body.get("pvMax"), p.getPvMax()));
        }
        if (body.containsKey("inimigo")) {
            p.setInimigo(Boolean.parseBoolean(String.valueOf(body.get("inimigo"))));
        }
        return participanteRepository.save(p);
    }

    @DeleteMapping("/participantes/{pid}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removerParticipante(@PathVariable Long pid) {
        participanteRepository.deleteById(pid);
    }

    // ----- helpers -----
    private static String texto(Object v) {
        if (v == null) {
            return null;
        }
        String s = String.valueOf(v).trim();
        return s.isEmpty() ? null : s;
    }

    private static int inteiro(Object v, int padrao) {
        if (v == null || String.valueOf(v).isBlank()) {
            return padrao;
        }
        try {
            return (int) Double.parseDouble(String.valueOf(v));
        } catch (NumberFormatException e) {
            return padrao;
        }
    }

    private static Long longOuNull(Object v) {
        if (v == null || String.valueOf(v).isBlank()) {
            return null;
        }
        try {
            return (long) Double.parseDouble(String.valueOf(v));
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
