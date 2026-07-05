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
    private final HabilidadePersonagemRepository habilidadeRepository;
    private final PersonagemSnapshotRepository snapshotRepository;
    private final CampanhaPersonagemRepository campanhaPersonagemRepository;
    private final CampanhaRepository campanhaRepository;
    private final DonoService donoService;

    public PersonagemController(PersonagemService service,
                                PersonagemRepository personagemRepository,
                                ItemPersonagemRepository itemRepository,
                                AtaqueRepository ataqueRepository,
                                FeiticoPersonagemRepository feiticoRepository,
                                HabilidadePersonagemRepository habilidadeRepository,
                                PersonagemSnapshotRepository snapshotRepository,
                                CampanhaPersonagemRepository campanhaPersonagemRepository,
                                CampanhaRepository campanhaRepository,
                                DonoService donoService) {
        this.service = service;
        this.personagemRepository = personagemRepository;
        this.itemRepository = itemRepository;
        this.ataqueRepository = ataqueRepository;
        this.feiticoRepository = feiticoRepository;
        this.habilidadeRepository = habilidadeRepository;
        this.snapshotRepository = snapshotRepository;
        this.campanhaPersonagemRepository = campanhaPersonagemRepository;
        this.campanhaRepository = campanhaRepository;
        this.donoService = donoService;
    }

    /** Campanhas em que este personagem está vinculado (para o chat de resultados na ficha). */
    @GetMapping("/personagens/{id}/campanhas")
    public List<Map<String, Object>> campanhasDoPersonagem(@PathVariable Long id) {
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
        return service.listar(orgId);
    }

    @PostMapping("/organizacoes/{orgId}/personagens")
    @ResponseStatus(HttpStatus.CREATED)
    public PersonagemResponse criar(@PathVariable Long orgId,
                                    @org.springframework.security.core.annotation.AuthenticationPrincipal UsuarioPrincipal principal,
                                    @Valid @RequestBody CriarPersonagemRequest req) {
        return service.criar(orgId, principal != null ? principal.id() : null, req);
    }

    @GetMapping("/personagens/{id}")
    public PersonagemResponse buscar(@PathVariable Long id) {
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
                                        @Valid @RequestBody AtualizarPersonagemRequest req) {
        return service.atualizar(id, req);
    }

    @PatchMapping("/personagens/{id}/status")
    public PersonagemResponse atualizarStatus(@PathVariable Long id,
                                              @RequestBody AtualizarStatusRequest req) {
        return service.atualizarStatus(id, req.pvAtual(), req.pmAtual(), req.peAtual(),
                req.pvMax(), req.pmMax(), req.peMax());
    }

    /** Atualiza XP/nivel; sobe de nivel automaticamente pelo XP e devolve os ganhos (popup). */
    @PatchMapping("/personagens/{id}/progresso")
    public ProgressoResponse atualizarProgresso(@PathVariable Long id,
                                                @RequestBody ProgressoRequest req) {
        return service.atualizarProgresso(id, req.xpAtual(), req.nivel());
    }

    /** Apaga a ficha e todos os vinculos (inventario, ataques, magias, habilidades, snapshots, campanhas). */
    @DeleteMapping("/personagens/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Transactional
    public void apagar(@PathVariable Long id) {
        itemRepository.deleteAll(itemRepository.findByPersonagemId(id));
        ataqueRepository.deleteAll(ataqueRepository.findByPersonagemId(id));
        feiticoRepository.deleteAll(feiticoRepository.findByPersonagemId(id));
        habilidadeRepository.deleteAll(habilidadeRepository.findByPersonagemId(id));
        snapshotRepository.deleteAll(snapshotRepository.findByPersonagemIdOrderByCriadoEmDesc(id));
        campanhaPersonagemRepository.deleteAll(campanhaPersonagemRepository.findByPersonagemId(id));
        personagemRepository.deleteById(id);
    }

    @GetMapping("/personagens/{id}/export")
    public ExportPersonagemResponse exportar(@PathVariable Long id) {
        return service.exportar(id);
    }

    @GetMapping("/personagens/{id}/debug")
    public CalculoDebugResponse debug(@PathVariable Long id) {
        return service.debug(id);
    }

    @GetMapping("/personagens/{id}/snapshots")
    public List<SnapshotResponse> snapshots(@PathVariable Long id) {
        return service.snapshots(id);
    }

    @GetMapping("/personagens/{id}/auditoria")
    public List<AuditoriaResponse> auditoria(@PathVariable Long id) {
        return service.historicoAuditoria(id);
    }
}
