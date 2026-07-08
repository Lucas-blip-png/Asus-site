package com.asus.platform.web;

import com.asus.platform.domain.Personagem;
import com.asus.platform.repository.AtaqueRepository;
import com.asus.platform.repository.BencaoPersonagemRepository;
import com.asus.platform.repository.FeiticoPersonagemRepository;
import com.asus.platform.repository.HabilidadePersonagemRepository;
import com.asus.platform.repository.HabilidadeRepository;
import com.asus.platform.repository.ItemPersonagemRepository;
import com.asus.platform.repository.PersonagemRepository;
import com.asus.platform.security.UsuarioPrincipal;
import com.asus.platform.service.AcessoService;
import com.asus.platform.service.PersonagemService;
import com.asus.platform.web.dto.HabilidadeEscolhidaResponse;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * Compartilhamento da ficha por link público (read-only).
 * O dono gera/revoga um token; qualquer pessoa com o link vê a ficha completa.
 */
@RestController
@RequestMapping("/api")
public class FichaPublicaController {

    private final PersonagemRepository personagemRepository;
    private final PersonagemService personagemService;
    private final AcessoService acessoService;
    private final HabilidadeRepository habilidadeRepository;
    private final HabilidadePersonagemRepository escolhidasRepository;
    private final AtaqueRepository ataqueRepository;
    private final FeiticoPersonagemRepository feiticoRepository;
    private final BencaoPersonagemRepository bencaoRepository;
    private final ItemPersonagemRepository inventarioRepository;

    public FichaPublicaController(PersonagemRepository personagemRepository,
                                  PersonagemService personagemService,
                                  AcessoService acessoService,
                                  HabilidadeRepository habilidadeRepository,
                                  HabilidadePersonagemRepository escolhidasRepository,
                                  AtaqueRepository ataqueRepository,
                                  FeiticoPersonagemRepository feiticoRepository,
                                  BencaoPersonagemRepository bencaoRepository,
                                  ItemPersonagemRepository inventarioRepository) {
        this.personagemRepository = personagemRepository;
        this.personagemService = personagemService;
        this.acessoService = acessoService;
        this.habilidadeRepository = habilidadeRepository;
        this.escolhidasRepository = escolhidasRepository;
        this.ataqueRepository = ataqueRepository;
        this.feiticoRepository = feiticoRepository;
        this.bencaoRepository = bencaoRepository;
        this.inventarioRepository = inventarioRepository;
    }

    /** Gera (ou devolve) o token do link público. Só o dono. */
    @PostMapping("/personagens/{id}/compartilhar")
    public Map<String, String> compartilhar(@PathVariable Long id,
                                            @AuthenticationPrincipal UsuarioPrincipal principal) {
        acessoService.exigirDonoPersonagem(id, principal);
        Personagem p = personagemRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Personagem " + id + " nao encontrado"));
        if (p.getShareToken() == null || p.getShareToken().isBlank()) {
            p.setShareToken(UUID.randomUUID().toString().replace("-", ""));
            personagemRepository.save(p);
        }
        return Map.of("token", p.getShareToken(), "url", "/p/" + p.getShareToken());
    }

    /** Revoga o link público. Só o dono. */
    @DeleteMapping("/personagens/{id}/compartilhar")
    public Map<String, String> revogar(@PathVariable Long id,
                                       @AuthenticationPrincipal UsuarioPrincipal principal) {
        acessoService.exigirDonoPersonagem(id, principal);
        Personagem p = personagemRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Personagem " + id + " nao encontrado"));
        p.setShareToken(null);
        personagemRepository.save(p);
        return Map.of("status", "revogado");
    }

    /** Ficha completa (read-only) pelo token público — sem login. */
    @GetMapping("/publico/personagens/{token}")
    public Map<String, Object> fichaPublica(@PathVariable String token) {
        Personagem p = personagemRepository.findByShareToken(token)
                .orElseThrow(() -> new NotFoundException("Ficha nao encontrada (link revogado?)"));

        var catalogo = habilidadeRepository.findByGameSystemId(p.getGameSystemId()).stream()
                .collect(Collectors.toMap(com.asus.platform.domain.Habilidade::getCodigo, Function.identity(), (a, b) -> a));
        var habilidades = escolhidasRepository.findByPersonagemId(p.getId()).stream()
                .map(hp -> {
                    var h = catalogo.get(hp.getHabilidadeCodigo());
                    return h == null ? null : HabilidadeEscolhidaResponse.de(h, hp);
                })
                .filter(java.util.Objects::nonNull)
                .toList();

        Map<String, Object> resp = new HashMap<>();
        resp.put("personagem", personagemService.buscar(p.getId()));
        resp.put("habilidades", habilidades);
        resp.put("ataques", ataqueRepository.findByPersonagemId(p.getId()));
        resp.put("feiticos", feiticoRepository.findByPersonagemId(p.getId()));
        resp.put("bencaos", bencaoRepository.findByPersonagemId(p.getId()));
        resp.put("inventario", inventarioRepository.findByPersonagemId(p.getId()));
        return resp;
    }
}
