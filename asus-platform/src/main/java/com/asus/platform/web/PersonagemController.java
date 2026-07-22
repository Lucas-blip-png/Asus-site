package com.asus.platform.web;

import com.asus.platform.domain.Campanha;
import com.asus.platform.domain.Personagem;
import com.asus.platform.repository.AtaqueRepository;
import com.asus.platform.repository.CampanhaPersonagemRepository;
import com.asus.platform.repository.CampanhaRepository;
import com.asus.platform.repository.FeiticoPersonagemRepository;
import com.asus.platform.repository.HabilidadePersonagemRepository;
import com.asus.platform.repository.ItemPersonagemRepository;
import com.asus.platform.repository.PersonagemRepository;
import com.asus.platform.repository.PersonagemSnapshotRepository;
import com.asus.platform.security.UsuarioPrincipal;
import com.asus.platform.service.AcessoService;
import com.asus.platform.service.DonoService;
import com.asus.platform.service.PersonagemService;
import com.asus.platform.web.dto.AtualizarPersonagemRequest;
import com.asus.platform.web.dto.AtualizarStatusRequest;
import com.asus.platform.web.dto.AuditoriaResponse;
import com.asus.platform.web.dto.CalculoDebugResponse;
import com.asus.platform.web.dto.CriarPersonagemRequest;
import com.asus.platform.web.dto.ExportPersonagemResponse;
import com.asus.platform.web.dto.ImportPersonagemRequest;
import com.asus.platform.web.dto.PersonagemResponse;
import com.asus.platform.web.dto.ProgressoRequest;
import com.asus.platform.web.dto.ProgressoResponse;
import com.asus.platform.web.dto.SnapshotResponse;
import com.asus.platform.web.dto.StatusDto;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

/** Personagens (plano, secao 21.3). */
@RestController
@RequestMapping("/api")
public class PersonagemController {

    private final PersonagemService service;
    private final PersonagemRepository personagemRepository;
    private final ItemPersonagemRepository itemRepository;
    private final AtaqueRepository ataqueRepository;
    private final FeiticoPersonagemRepository feiticoRepository;
    private final com.asus.platform.repository.BencaoPersonagemRepository bencaoRepository;
    private final HabilidadePersonagemRepository habilidadeRepository;
    private final PersonagemSnapshotRepository snapshotRepository;
    private final CampanhaPersonagemRepository campanhaPersonagemRepository;
    private final CampanhaRepository campanhaRepository;
    private final DonoService donoService;
    private final AcessoService acessoService;

    public PersonagemController(PersonagemService service,
                                PersonagemRepository personagemRepository,
                                ItemPersonagemRepository itemRepository,
                                AtaqueRepository ataqueRepository,
                                FeiticoPersonagemRepository feiticoRepository,
                                com.asus.platform.repository.BencaoPersonagemRepository bencaoRepository,
                                HabilidadePersonagemRepository habilidadeRepository,
                                PersonagemSnapshotRepository snapshotRepository,
                                CampanhaPersonagemRepository campanhaPersonagemRepository,
                                CampanhaRepository campanhaRepository,
                                DonoService donoService,
                                AcessoService acessoService) {
        this.service = service;
        this.personagemRepository = personagemRepository;
        this.itemRepository = itemRepository;
        this.ataqueRepository = ataqueRepository;
        this.feiticoRepository = feiticoRepository;
        this.bencaoRepository = bencaoRepository;
        this.habilidadeRepository = habilidadeRepository;
        this.snapshotRepository = snapshotRepository;
        this.campanhaPersonagemRepository = campanhaPersonagemRepository;
        this.campanhaRepository = campanhaRepository;
        this.donoService = donoService;
        this.acessoService = acessoService;
    }

    /** Campanhas em que este personagem está vinculado (para o chat de resultados na ficha). */
    @GetMapping("/personagens/{id}/campanhas")
    public List<Map<String, Object>> campanhasDoPersonagem(@PathVariable Long id,
                                                           @org.springframework.security.core.annotation.AuthenticationPrincipal UsuarioPrincipal principal) {
        acessoService.exigirDonoOuMestrePersonagem(id, principal);
        return campanhaPersonagemRepository.findByPersonagemId(id).stream()
                .map(cp -> campanhaRepository.findById(cp.getCampanhaId()).orElse(null))
                .filter(c -> c != null)
                .map(c -> {
                    Map<String, Object> m = new java.util.HashMap<>();
                    m.put("id", c.getId());
                    m.put("nome", c.getNome());
                    m.put("mestreId", c.getMestreId());
                    return m;
                })
                .toList();
    }

    @GetMapping("/organizacoes/{orgId}/personagens")
    public List<PersonagemResponse> listar(@PathVariable Long orgId,
                                           @org.springframework.security.core.annotation.AuthenticationPrincipal UsuarioPrincipal principal) {
        // O dono/dev vê todos os personagens (acesso total).
        if (principal != null && donoService.ehDono(principal.id())) {
            return service.listarTodos();
        }
        List<PersonagemResponse> lista = service.listar(orgId);
        if (principal == null) {
            return lista;
        }
        // Fichas privadas: cada jogador ve as proprias; o mestre ve tambem as das suas mesas.
        java.util.Set<Long> meus = new java.util.HashSet<>();
        personagemRepository.findByUsuarioId(principal.id()).forEach(p -> meus.add(p.getId()));
        meus.addAll(acessoService.personagensDasMinhasMesas(principal.id()));
        return lista.stream().filter(r -> meus.contains(r.id())).toList();
    }

    @PostMapping("/organizacoes/{orgId}/personagens")
    @ResponseStatus(HttpStatus.CREATED)
    public PersonagemResponse criar(@PathVariable Long orgId,
                                    @org.springframework.security.core.annotation.AuthenticationPrincipal UsuarioPrincipal principal,
                                    @Valid @RequestBody CriarPersonagemRequest req) {
        return service.criar(orgId, principal != null ? principal.id() : null, req);
    }

    @GetMapping("/personagens/{id}")
    public PersonagemResponse buscar(@PathVariable Long id,
                                     @org.springframework.security.core.annotation.AuthenticationPrincipal UsuarioPrincipal principal) {
        acessoService.exigirDonoOuMestrePersonagem(id, principal);
        return service.buscar(id);
    }

    /** Info minima e publica para o overlay OBS por personagem (nome + retrato). */
    @GetMapping("/personagens/{id}/overlay")
    public Map<String, Object> overlay(@PathVariable Long id) {
        Personagem p = personagemRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Personagem " + id + " nao encontrado"));
        Map<String, Object> m = new java.util.HashMap<>();
        m.put("id", p.getId());
        m.put("nome", p.getNome());
        m.put("avatarAssetId", p.getAvatarAssetId());
        m.put("status", StatusDto.de(p.getStatus()));
        return m;
    }

    @PostMapping("/personagens/import")
    @ResponseStatus(HttpStatus.CREATED)
    public PersonagemResponse importar(@Valid @RequestBody ImportPersonagemRequest req) {
        return service.importar(req);
    }

    @PutMapping("/personagens/{id}")
    public PersonagemResponse atualizar(@PathVariable Long id,
                                        @org.springframework.security.core.annotation.AuthenticationPrincipal UsuarioPrincipal principal,
                                        @Valid @RequestBody AtualizarPersonagemRequest req) {
        acessoService.exigirDonoOuMestrePersonagem(id, principal);
        return service.atualizar(id, req);
    }

    @PatchMapping("/personagens/{id}/status")
    public PersonagemResponse atualizarStatus(@PathVariable Long id,
                                              @org.springframework.security.core.annotation.AuthenticationPrincipal UsuarioPrincipal principal,
                                              @RequestBody AtualizarStatusRequest req) {
        acessoService.exigirDonoOuMestrePersonagem(id, principal);
        return service.atualizarStatus(id, req.pvAtual(), req.pmAtual(), req.peAtual(),
                req.pvMax(), req.pmMax(), req.peMax());
    }

    /** Atualiza XP/nivel; sobe de nivel automaticamente pelo XP e devolve os ganhos (popup). */
    @PatchMapping("/personagens/{id}/progresso")
    public ProgressoResponse atualizarProgresso(@PathVariable Long id,
                                                @org.springframework.security.core.annotation.AuthenticationPrincipal UsuarioPrincipal principal,
                                                @RequestBody ProgressoRequest req) {
        acessoService.exigirDonoOuMestrePersonagem(id, principal);
        return service.atualizarProgresso(id, req.xpAtual(), req.nivel());
    }

    /** Apaga a ficha e todos os vinculos (inventario, ataques, magias, habilidades, snapshots, campanhas). */
    @DeleteMapping("/personagens/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Transactional
    public void apagar(@PathVariable Long id,
                       @org.springframework.security.core.annotation.AuthenticationPrincipal UsuarioPrincipal principal) {
        acessoService.exigirDonoPersonagem(id, principal);
        itemRepository.deleteAll(itemRepository.findByPersonagemId(id));
        ataqueRepository.deleteAll(ataqueRepository.findByPersonagemId(id));
        feiticoRepository.deleteAll(feiticoRepository.findByPersonagemId(id));
        bencaoRepository.deleteAll(bencaoRepository.findByPersonagemId(id));
        habilidadeRepository.deleteAll(habilidadeRepository.findByPersonagemId(id));
        snapshotRepository.deleteAll(snapshotRepository.findByPersonagemIdOrderByCriadoEmDesc(id));
        campanhaPersonagemRepository.deleteAll(campanhaPersonagemRepository.findByPersonagemId(id));
        personagemRepository.deleteById(id);
    }

    @GetMapping("/personagens/{id}/export")
    public ExportPersonagemResponse exportar(@PathVariable Long id,
            @org.springframework.security.core.annotation.AuthenticationPrincipal UsuarioPrincipal principal) {
        acessoService.exigirDonoOuMestrePersonagem(id, principal);
        return service.exportar(id);
    }

    @GetMapping("/personagens/{id}/debug")
    public CalculoDebugResponse debug(@PathVariable Long id,
            @org.springframework.security.core.annotation.AuthenticationPrincipal UsuarioPrincipal principal) {
        acessoService.exigirDonoOuMestrePersonagem(id, principal);
        return service.debug(id);
    }

    @GetMapping("/personagens/{id}/snapshots")
    public List<SnapshotResponse> snapshots(@PathVariable Long id,
            @org.springframework.security.core.annotation.AuthenticationPrincipal UsuarioPrincipal principal) {
        acessoService.exigirDonoOuMestrePersonagem(id, principal);
        return service.snapshots(id);
    }

    @GetMapping("/personagens/{id}/auditoria")
    public List<AuditoriaResponse> auditoria(@PathVariable Long id,
            @org.springframework.security.core.annotation.AuthenticationPrincipal UsuarioPrincipal principal) {
        acessoService.exigirDonoOuMestrePersonagem(id, principal);
        return service.historicoAuditoria(id);
    }
}
