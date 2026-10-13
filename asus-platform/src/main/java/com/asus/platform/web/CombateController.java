package com.asus.platform.web;

import com.asus.platform.domain.Combate;
import com.asus.platform.domain.CombateParticipante;
import com.asus.platform.domain.Permissao;
import com.asus.platform.repository.CombateParticipanteRepository;
import com.asus.platform.repository.CombateRepository;
import com.asus.platform.security.UsuarioPrincipal;
import com.asus.platform.service.CampanhaService;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

/** Combates (rastreador de iniciativa) de uma campanha. */
@RestController
@RequestMapping("/api")
public class CombateController {

    private final CombateRepository combateRepository;
    private final CombateParticipanteRepository participanteRepository;
    private final CampanhaService campanhaService;

    public CombateController(CombateRepository combateRepository,
                             CombateParticipanteRepository participanteRepository,
                             CampanhaService campanhaService) {
        this.combateRepository = combateRepository;
        this.participanteRepository = participanteRepository;
        this.campanhaService = campanhaService;
    }

    /** Ler = ser membro da campanha; alterar = gerenciar a campanha. Anonimo (dev aberto) nao e bloqueado. */
    private void exigir(Long campanhaId, UsuarioPrincipal principal, Permissao permissao) {
        if (principal != null) {
            campanhaService.exigirPermissao(campanhaId, principal.id(), permissao);
        }
    }

    private Combate carregar(Long id) {
        return combateRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Combate " + id + " nao encontrado"));
    }

    private CombateParticipante carregarParticipante(Long pid) {
        return participanteRepository.findById(pid)
                .orElseThrow(() -> new NotFoundException("Participante " + pid + " nao encontrado"));
    }

    @GetMapping("/campanhas/{campanhaId}/combates")
    public List<Combate> listar(@PathVariable Long campanhaId,
                                @AuthenticationPrincipal UsuarioPrincipal principal) {
        exigir(campanhaId, principal, Permissao.VER_FICHA);
        return combateRepository.findByCampanhaIdOrderByCriadoEmDesc(campanhaId);
    }

    @PostMapping("/campanhas/{campanhaId}/combates")
    @ResponseStatus(HttpStatus.CREATED)
    public Combate criar(@PathVariable Long campanhaId, @AuthenticationPrincipal UsuarioPrincipal principal,
                         @RequestBody Map<String, Object> body) {
        exigir(campanhaId, principal, Permissao.GERENCIAR_CAMPANHA);
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
    public Combate buscar(@PathVariable Long id, @AuthenticationPrincipal UsuarioPrincipal principal) {
        Combate c = carregar(id);
        exigir(c.getCampanhaId(), principal, Permissao.VER_FICHA);
        return c;
    }

    @PutMapping("/combates/{id}")
    public Combate atualizar(@PathVariable Long id, @AuthenticationPrincipal UsuarioPrincipal principal,
                             @RequestBody Map<String, Object> body) {
        Combate c = carregar(id);
        exigir(c.getCampanhaId(), principal, Permissao.GERENCIAR_CAMPANHA);
        if (body.containsKey("nome") && texto(body.get("nome")) != null) {
            c.setNome(texto(body.get("nome")));
        }
        if (body.containsKey("ativo")) {
            c.setAtivo(Boolean.parseBoolean(String.valueOf(body.get("ativo"))));
        }
        // Mapa tático: imagem de fundo (null remove) e névoa (JSON das células reveladas; null desliga).
        if (body.containsKey("mapaAssetId")) {
            Object v = body.get("mapaAssetId");
            c.setMapaAssetId(v == null ? null : (long) Double.parseDouble(String.valueOf(v)));
        }
        if (body.containsKey("fogJson")) {
            Object v = body.get("fogJson");
            c.setFogJson(v == null ? null : String.valueOf(v));
        }
        return combateRepository.save(c);
    }

    @DeleteMapping("/combates/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Transactional
    public void apagar(@PathVariable Long id, @AuthenticationPrincipal UsuarioPrincipal principal) {
        exigir(carregar(id).getCampanhaId(), principal, Permissao.GERENCIAR_CAMPANHA);
        participanteRepository.deleteAll(participanteRepository.findByCombateIdOrderByIniciativaDescIdAsc(id));
        combateRepository.deleteById(id);
    }

    /** Avança o turno; ao passar do último participante, incrementa a rodada. */
    @PostMapping("/combates/{id}/proximo")
    public Combate proximoTurno(@PathVariable Long id, @AuthenticationPrincipal UsuarioPrincipal principal) {
        Combate c = carregar(id);
        exigir(c.getCampanhaId(), principal, Permissao.GERENCIAR_CAMPANHA);
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
    public Combate reset(@PathVariable Long id, @AuthenticationPrincipal UsuarioPrincipal principal) {
        Combate c = carregar(id);
        exigir(c.getCampanhaId(), principal, Permissao.GERENCIAR_CAMPANHA);
        c.setRodada(1);
        c.setTurnoAtual(0);
        return combateRepository.save(c);
    }

    @GetMapping("/combates/{id}/participantes")
    public List<CombateParticipante> participantes(@PathVariable Long id,
                                                   @AuthenticationPrincipal UsuarioPrincipal principal) {
        exigir(carregar(id).getCampanhaId(), principal, Permissao.VER_FICHA);
        return participanteRepository.findByCombateIdOrderByIniciativaDescIdAsc(id);
    }

    @PostMapping("/combates/{id}/participantes")
    @ResponseStatus(HttpStatus.CREATED)
    public CombateParticipante adicionar(@PathVariable Long id, @AuthenticationPrincipal UsuarioPrincipal principal,
                                         @RequestBody Map<String, Object> body) {
        exigir(carregar(id).getCampanhaId(), principal, Permissao.GERENCIAR_CAMPANHA);
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
    public CombateParticipante atualizarParticipante(@PathVariable Long pid,
                                                     @AuthenticationPrincipal UsuarioPrincipal principal,
                                                     @RequestBody Map<String, Object> body) {
        CombateParticipante p = carregarParticipante(pid);
        exigir(carregar(p.getCombateId()).getCampanhaId(), principal, Permissao.GERENCIAR_CAMPANHA);
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
        if (body.containsKey("condicoes")) {
            Object cond = body.get("condicoes");
            p.setCondicoes(cond == null ? null : String.valueOf(cond));
        }
        // Posição no mapa tático (null tira do mapa).
        if (body.containsKey("posX")) {
            p.setPosX(body.get("posX") == null ? null : inteiro(body.get("posX"), 0));
        }
        if (body.containsKey("posY")) {
            p.setPosY(body.get("posY") == null ? null : inteiro(body.get("posY"), 0));
        }
        return participanteRepository.save(p);
    }

    @DeleteMapping("/participantes/{pid}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removerParticipante(@PathVariable Long pid, @AuthenticationPrincipal UsuarioPrincipal principal) {
        exigir(carregar(carregarParticipante(pid).getCombateId()).getCampanhaId(), principal,
                Permissao.GERENCIAR_CAMPANHA);
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
