package com.asus.platform.service;

import com.asus.platform.domain.Classe;
import com.asus.platform.domain.GameSystem;
import com.asus.platform.domain.Personagem;
import com.asus.platform.domain.Raca;
import com.asus.platform.domain.Status;
import com.asus.platform.engine.AsusV1Engine;
import com.asus.platform.engine.PericiaCalculada;
import com.asus.platform.engine.ResultadoCalculo;
import com.asus.platform.realtime.RealtimeNotifier;
import com.asus.platform.repository.ClasseRepository;
import com.asus.platform.repository.GameSystemRepository;
import com.asus.platform.repository.PersonagemRepository;
import com.asus.platform.repository.RacaRepository;
import com.asus.platform.web.NotFoundException;
import com.asus.platform.web.dto.AtributosDto;
import com.asus.platform.web.dto.AtualizarPersonagemRequest;
import com.asus.platform.web.dto.AuditoriaResponse;
import com.asus.platform.web.dto.CalculoDebugResponse;
import com.asus.platform.web.dto.CriarPersonagemRequest;
import com.asus.platform.web.dto.ExportPersonagemResponse;
import com.asus.platform.web.dto.ImportPersonagemRequest;
import com.asus.platform.web.dto.PericiaCalculadaDto;
import com.asus.platform.web.dto.PersonagemResponse;
import com.asus.platform.web.dto.SnapshotResponse;
import com.asus.platform.web.dto.StatusDto;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PersonagemService {

    private final PersonagemRepository personagemRepository;
    private final GameSystemRepository gameSystemRepository;
    private final RacaRepository racaRepository;
    private final ClasseRepository classeRepository;
    private final CalculoService calculoService;
    private final SnapshotService snapshotService;
    private final AuditoriaService auditoriaService;
    private final OrganizacaoService organizacaoService;
    private final RealtimeNotifier realtimeNotifier;
    private final PlanoService planoService;

    public PersonagemService(PersonagemRepository personagemRepository,
                             GameSystemRepository gameSystemRepository,
                             RacaRepository racaRepository,
                             ClasseRepository classeRepository,
                             CalculoService calculoService,
                             SnapshotService snapshotService,
                             AuditoriaService auditoriaService,
                             OrganizacaoService organizacaoService,
                             RealtimeNotifier realtimeNotifier,
                             PlanoService planoService) {
        this.personagemRepository = personagemRepository;
        this.gameSystemRepository = gameSystemRepository;
        this.racaRepository = racaRepository;
        this.classeRepository = classeRepository;
        this.calculoService = calculoService;
        this.snapshotService = snapshotService;
        this.auditoriaService = auditoriaService;
        this.organizacaoService = organizacaoService;
        this.realtimeNotifier = realtimeNotifier;
        this.planoService = planoService;
    }

    public List<PersonagemResponse> listar(Long organizacaoId) {
        organizacaoService.buscar(organizacaoId); // valida existencia
        return personagemRepository.findByOrganizacaoIdAndArquivadoFalse(organizacaoId)
                .stream().map(this::toResponse).toList();
    }

    public PersonagemResponse buscar(Long id) {
        return toResponse(carregar(id));
    }

    @Transactional
    public PersonagemResponse criar(Long organizacaoId, CriarPersonagemRequest req) {
        organizacaoService.buscar(organizacaoId); // valida existencia
        planoService.validarNovoPersonagem(organizacaoId); // Fase 10: limite do plano

        Personagem p = montarSalvar(organizacaoId, req.nome(), req.jogador(),
                req.racaCodigo(), req.classeCodigo(), req.nivelOuPadrao(), 0, req.atributosBase());

        // Snapshot na criacao (criterio 8)
        snapshotService.criar(p, "CRIACAO");

        // Auditoria (criterio 7)
        auditoriaService.registrar(organizacaoId, p.getUsuarioId(), "PERSONAGEM_CRIADO",
                "Personagem", p.getId(), null, null, p.getNome());

        return toResponse(p);
    }

    /** Importa um personagem a partir do envelope de export (plano, secao 12.2). */
    @Transactional
    public PersonagemResponse importar(ImportPersonagemRequest req) {
        ImportPersonagemRequest.PersonagemImport dados = req.personagem();
        organizacaoService.buscar(dados.organizacaoId()); // valida existencia
        planoService.validarNovoPersonagem(dados.organizacaoId()); // Fase 10: limite do plano

        Personagem p = montarSalvar(dados.organizacaoId(), dados.nome(), dados.jogador(),
                dados.racaCodigo(), dados.classeCodigo(),
                dados.nivelOuPadrao(), dados.xpOuZero(), dados.atributosBase());

        snapshotService.criar(p, "IMPORTACAO");
        auditoriaService.registrar(dados.organizacaoId(), p.getUsuarioId(), "PERSONAGEM_IMPORTADO",
                "Personagem", p.getId(), null, null, p.getNome());

        return toResponse(p);
    }

    /** Atualiza a ficha (plano, secao 21.3 — PUT): recalcula, preserva dano e snapshota. */
    @Transactional
    public PersonagemResponse atualizar(Long id, AtualizarPersonagemRequest req) {
        Personagem p = carregar(id);
        int nivelAntigo = p.getNivel();

        if (req.nome() != null) {
            p.setNome(req.nome());
        }
        if (req.jogador() != null) {
            p.setJogador(req.jogador());
        }
        if (req.xpAtual() != null) {
            p.setXpAtual(req.xpAtual());
        }
        if (req.nivel() != null && req.nivel() >= 1) {
            p.setNivel(req.nivel());
        }
        if (req.atributosBase() != null) {
            p.setAtributosBase(req.atributosBase().paraEntidade());
        }
        if (req.anotacoes() != null) {
            p.setAnotacoes(req.anotacoes());
        }
        if (req.aparencia() != null) {
            p.setAparencia(req.aparencia());
        }
        if (req.personalidade() != null) {
            p.setPersonalidade(req.personalidade());
        }
        if (req.historico() != null) {
            p.setHistorico(req.historico());
        }
        if (req.objetivo() != null) {
            p.setObjetivo(req.objetivo());
        }

        GameSystem asus = gameSystemRepository.findByCodigo(AsusV1Engine.SYSTEM_ID)
                .orElseThrow(() -> new NotFoundException("Sistema ASUS nao encontrado"));
        if (req.racaCodigo() != null) {
            Raca raca = racaRepository.findByGameSystemIdAndCodigo(asus.getId(), req.racaCodigo())
                    .orElseThrow(() -> new NotFoundException("Raca '" + req.racaCodigo() + "' nao encontrada"));
            p.setRacaId(raca.getId());
        }
        if (req.classeCodigo() != null) {
            Classe classe = classeRepository.findByGameSystemIdAndCodigo(asus.getId(), req.classeCodigo())
                    .orElseThrow(() -> new NotFoundException("Classe '" + req.classeCodigo() + "' nao encontrada"));
            p.setClasseId(classe.getId());
        }

        // Recalcula a ficha preservando o "dano" atual (atual <= novo maximo).
        ResultadoCalculo r = calculoService.calcular(p);
        Status novo = r.status();
        Status antigo = p.getStatus();
        if (antigo != null) {
            novo.setPvAtual(Math.min(antigo.getPvAtual(), novo.getPvMax()));
            novo.setPmAtual(Math.min(antigo.getPmAtual(), novo.getPmMax()));
            novo.setPeAtual(Math.min(antigo.getPeAtual(), novo.getPeMax()));
        }
        p.setAtributosFinais(r.atributosFinais());
        p.setStatus(novo);
        p = personagemRepository.save(p);

        boolean subiuNivel = p.getNivel() > nivelAntigo;
        snapshotService.criar(p, subiuNivel ? "LEVEL_UP" : "EDICAO");
        auditoriaService.registrar(p.getOrganizacaoId(), p.getUsuarioId(), "PERSONAGEM_ATUALIZADO",
                "Personagem", p.getId(),
                subiuNivel ? "nivel" : null,
                subiuNivel ? String.valueOf(nivelAntigo) : null,
                subiuNivel ? String.valueOf(p.getNivel()) : p.getNome());

        realtimeNotifier.statusPersonagem(p.getId(), StatusDto.de(p.getStatus()));
        return toResponse(p);
    }

    public List<AuditoriaResponse> historicoAuditoria(Long id) {
        carregar(id); // valida existencia
        return auditoriaService.historico("Personagem", id).stream()
                .map(AuditoriaResponse::de).toList();
    }

    /** Resolve ASUS/raca/classe, monta a ficha, calcula status e persiste. */
    private Personagem montarSalvar(Long organizacaoId, String nome, String jogador,
                                    String racaCodigo, String classeCodigo,
                                    int nivel, int xpAtual, AtributosDto atributosBase) {
        GameSystem asus = gameSystemRepository.findByCodigo(AsusV1Engine.SYSTEM_ID)
                .orElseThrow(() -> new NotFoundException("Sistema ASUS nao encontrado"));

        Raca raca = racaRepository.findByGameSystemIdAndCodigo(asus.getId(), racaCodigo)
                .orElseThrow(() -> new NotFoundException("Raca '" + racaCodigo + "' nao encontrada"));

        Classe classe = classeRepository.findByGameSystemIdAndCodigo(asus.getId(), classeCodigo)
                .orElseThrow(() -> new NotFoundException("Classe '" + classeCodigo + "' nao encontrada"));

        Personagem p = Personagem.builder()
                .organizacaoId(organizacaoId)
                .gameSystemId(asus.getId())
                .rulesetVersion(AsusV1Engine.VERSION)
                .nome(nome)
                .jogador(jogador)
                .racaId(raca.getId())
                .classeId(classe.getId())
                .nivel(nivel)
                .xpAtual(xpAtual)
                .atributosBase(atributosBase.paraEntidade())
                .arquivado(false)
                .build();

        // Calculo automatico da ficha (criterio 3)
        ResultadoCalculo r = calculoService.calcular(p);
        p.setAtributosFinais(r.atributosFinais());
        p.setStatus(r.status());

        return personagemRepository.save(p);
    }

    @Transactional
    public PersonagemResponse atualizarStatus(Long id, Integer pvAtual, Integer pmAtual, Integer peAtual) {
        Personagem p = carregar(id);
        var status = p.getStatus();

        if (pvAtual != null && pvAtual != status.getPvAtual()) {
            auditoriaService.registrar(p.getOrganizacaoId(), p.getUsuarioId(), "STATUS_ALTERADO",
                    "Personagem", id, "pvAtual", String.valueOf(status.getPvAtual()), String.valueOf(pvAtual));
            status.setPvAtual(pvAtual);
        }
        if (pmAtual != null && pmAtual != status.getPmAtual()) {
            auditoriaService.registrar(p.getOrganizacaoId(), p.getUsuarioId(), "STATUS_ALTERADO",
                    "Personagem", id, "pmAtual", String.valueOf(status.getPmAtual()), String.valueOf(pmAtual));
            status.setPmAtual(pmAtual);
        }
        if (peAtual != null && peAtual != status.getPeAtual()) {
            auditoriaService.registrar(p.getOrganizacaoId(), p.getUsuarioId(), "STATUS_ALTERADO",
                    "Personagem", id, "peAtual", String.valueOf(status.getPeAtual()), String.valueOf(peAtual));
            status.setPeAtual(peAtual);
        }

        p = personagemRepository.save(p);
        realtimeNotifier.statusPersonagem(p.getId(), StatusDto.de(p.getStatus()));
        return toResponse(p);
    }

    public ExportPersonagemResponse exportar(Long id) {
        return ExportPersonagemResponse.de(toResponse(carregar(id)));
    }

    public CalculoDebugResponse debug(Long id) {
        Personagem p = carregar(id);
        ResultadoCalculo r = calculoService.calcular(p);
        return new CalculoDebugResponse(
                AtributosDto.de(p.getAtributosBase()),
                AtributosDto.de(r.atributosFinais()),
                StatusDto.de(r.status()),
                r.pericias().stream().map(this::toPericiaDto).toList(),
                r.passos());
    }

    public List<SnapshotResponse> snapshots(Long id) {
        carregar(id); // valida existencia
        return snapshotService.listar(id).stream().map(SnapshotResponse::de).toList();
    }

    // ----- helpers -----

    private Personagem carregar(Long id) {
        return personagemRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Personagem " + id + " nao encontrado"));
    }

    private PersonagemResponse toResponse(Personagem p) {
        GameSystem sistema = gameSystemRepository.findById(p.getGameSystemId()).orElse(null);
        Raca raca = p.getRacaId() == null ? null : racaRepository.findById(p.getRacaId()).orElse(null);
        Classe classe = p.getClasseId() == null ? null : classeRepository.findById(p.getClasseId()).orElse(null);

        // pericias sao recalculadas (nao tem estado "atual" para persistir)
        ResultadoCalculo r = calculoService.calcular(p);
        List<PericiaCalculadaDto> pericias = r.pericias().stream().map(this::toPericiaDto).toList();

        return new PersonagemResponse(
                p.getId(),
                p.getOrganizacaoId(),
                p.getNome(),
                p.getJogador(),
                sistema == null ? null : sistema.getCodigo(),
                p.getRulesetVersion(),
                raca == null ? null : raca.getCodigo(),
                raca == null ? null : raca.getNome(),
                classe == null ? null : classe.getCodigo(),
                classe == null ? null : classe.getNome(),
                p.getNivel(),
                p.getXpAtual(),
                AtributosDto.de(p.getAtributosBase()),
                AtributosDto.de(p.getAtributosFinais()),
                StatusDto.de(p.getStatus()),
                pericias,
                p.isArquivado(),
                p.getCriadoEm(),
                p.getAtualizadoEm());
    }

    private PericiaCalculadaDto toPericiaDto(PericiaCalculada pc) {
        return new PericiaCalculadaDto(pc.codigo(), pc.nome(), pc.atributoBase(), pc.valor());
    }
}
