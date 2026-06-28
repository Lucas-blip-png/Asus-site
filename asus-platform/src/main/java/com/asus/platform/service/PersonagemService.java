package com.asus.platform.service;

import com.asus.platform.domain.Classe;
import com.asus.platform.domain.Atributo;
import com.asus.platform.domain.GameSystem;
import com.asus.platform.domain.ItemPersonagem;
import com.asus.platform.domain.Personagem;
import com.asus.platform.domain.ProgressaoNivel;
import com.asus.platform.domain.Raca;
import com.asus.platform.domain.Status;
import com.asus.platform.engine.AsusV1Engine;
import com.asus.platform.engine.PericiaCalculada;
import com.asus.platform.engine.ResultadoCalculo;
import com.asus.platform.realtime.RealtimeNotifier;
import com.asus.platform.repository.ClasseRepository;
import com.asus.platform.repository.GameSystemRepository;
import com.asus.platform.repository.ItemPersonagemRepository;
import com.asus.platform.repository.PersonagemRepository;
import com.asus.platform.repository.ProgressaoNivelRepository;
import com.asus.platform.repository.RacaRepository;
import com.asus.platform.web.NotFoundException;
import com.asus.platform.web.dto.AtributosDto;
import com.asus.platform.web.dto.AtualizarPersonagemRequest;
import com.asus.platform.web.dto.AuditoriaResponse;
import com.asus.platform.web.dto.CalculoDebugResponse;
import com.asus.platform.web.dto.CriarPersonagemRequest;
import com.asus.platform.web.dto.ExportPersonagemResponse;
import com.asus.platform.web.dto.ImportPersonagemRequest;
import com.asus.platform.web.dto.NivelGanho;
import com.asus.platform.web.dto.PericiaCalculadaDto;
import com.asus.platform.web.dto.PericiaCustomDto;
import com.asus.platform.web.dto.PersonagemResponse;
import com.asus.platform.web.dto.ProgressoResponse;
import com.asus.platform.web.dto.SnapshotResponse;
import com.asus.platform.web.dto.StatusDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
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
    private final ProgressaoNivelRepository progressaoNivelRepository;
    private final ItemPersonagemRepository itemPersonagemRepository;
    private final ObjectMapper objectMapper;

    public PersonagemService(PersonagemRepository personagemRepository,
                             GameSystemRepository gameSystemRepository,
                             RacaRepository racaRepository,
                             ClasseRepository classeRepository,
                             CalculoService calculoService,
                             SnapshotService snapshotService,
                             AuditoriaService auditoriaService,
                             OrganizacaoService organizacaoService,
                             RealtimeNotifier realtimeNotifier,
                             PlanoService planoService,
                             ProgressaoNivelRepository progressaoNivelRepository,
                             ItemPersonagemRepository itemPersonagemRepository,
                             ObjectMapper objectMapper) {
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
        this.progressaoNivelRepository = progressaoNivelRepository;
        this.itemPersonagemRepository = itemPersonagemRepository;
        this.objectMapper = objectMapper;
    }

    public List<PersonagemResponse> listar(Long organizacaoId) {
        organizacaoService.buscar(organizacaoId); // valida existencia
        return personagemRepository.findByOrganizacaoIdAndArquivadoFalse(organizacaoId)
                .stream().map(this::toResponse).toList();
    }

    /** Todos os personagens (uso do dono/dev — acesso total). */
    public List<PersonagemResponse> listarTodos() {
        return personagemRepository.findAll().stream()
                .filter(p -> !p.isArquivado())
                .map(this::toResponse).toList();
    }

    public PersonagemResponse buscar(Long id) {
        return toResponse(carregar(id));
    }

    @Transactional
    public PersonagemResponse criar(Long organizacaoId, Long usuarioId, CriarPersonagemRequest req) {
        organizacaoService.buscar(organizacaoId);
        planoService.validarNovoPersonagem(organizacaoId);

        int nivelInicial = req.nivel() == null ? 1 : Math.max(1, req.nivel());
        validarDistribuicaoCriacao(req.atributosBase(), req.pericias(), nivelInicial,
                req.classeCodigo(), req.trilhaCodigo());
        Personagem p = montarSalvar(organizacaoId, usuarioId, req.nome(), req.jogador(),
                req.racaCodigo(), req.classeCodigo(), req.trilhaCodigo(), req.divindade(),
                nivelInicial, 0, req.atributosBase(), req.pericias());

        snapshotService.criar(p, "CRIACAO");
        auditoriaService.registrar(organizacaoId, p.getUsuarioId(), "PERSONAGEM_CRIADO",
                "Personagem", p.getId(), null, null, p.getNome());

        return toResponse(p);
    }

    /** Importa um personagem a partir do envelope de export (plano, secao 12.2). */
    @Transactional
    public PersonagemResponse importar(ImportPersonagemRequest req) {
        ImportPersonagemRequest.PersonagemImport dados = req.personagem();
        organizacaoService.buscar(dados.organizacaoId());
        planoService.validarNovoPersonagem(dados.organizacaoId());

        Personagem p = montarSalvar(dados.organizacaoId(), null, dados.nome(), dados.jogador(),
                dados.racaCodigo(), dados.classeCodigo(), dados.trilhaCodigo(), dados.divindade(),
                dados.nivelOuPadrao(), dados.xpOuZero(), dados.atributosBase(), null);

        snapshotService.criar(p, "IMPORTACAO");
        auditoriaService.registrar(dados.organizacaoId(), p.getUsuarioId(), "PERSONAGEM_IMPORTADO",
                "Personagem", p.getId(), null, null, p.getNome());

        return toResponse(p);
    }

    /** Atualiza a ficha (PUT): recalcula, preserva dano e snapshota. */
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
        if (req.avatarAssetId() != null) {
            p.setAvatarAssetId(req.avatarAssetId() < 0 ? null : req.avatarAssetId());
        }
        if (req.atributosBase() != null) {
            String classeCod = p.getClasseId() == null ? null
                    : classeRepository.findById(p.getClasseId()).map(Classe::getCodigo).orElse(null);
            String trilhaCod = p.getTrilhaId() == null ? null
                    : classeRepository.findById(p.getTrilhaId()).map(Classe::getCodigo).orElse(null);
            validarTetoAtributo(req.atributosBase(), p.getNivel(), classeCod, trilhaCod);
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

        GameSystem asus = asus();
        if (req.racaCodigo() != null) {
            p.setRacaId(racaRepository.findByGameSystemIdAndCodigo(asus.getId(), req.racaCodigo())
                    .map(Raca::getId)
                    .orElseThrow(() -> new NotFoundException("Raca '" + req.racaCodigo() + "' nao encontrada")));
        }
        if (req.classeCodigo() != null) {
            p.setClasseId(classeRepository.findByGameSystemIdAndCodigo(asus.getId(), req.classeCodigo())
                    .map(Classe::getId)
                    .orElseThrow(() -> new NotFoundException("Classe '" + req.classeCodigo() + "' nao encontrada")));
        }
        if (req.trilhaCodigo() != null) {
            p.setTrilhaId(req.trilhaCodigo().isBlank() ? null
                    : classeRepository.findByGameSystemIdAndCodigo(asus.getId(), req.trilhaCodigo())
                            .map(Classe::getId)
                            .orElseThrow(() -> new NotFoundException("Trilha '" + req.trilhaCodigo() + "' nao encontrada")));
        }
        if (req.divindade() != null) {
            p.setDivindade(req.divindade());
        }
        if (req.pericias() != null) {
            p.setJsonPericias(serializar(req.pericias()));
        }
        if (req.periciasCustom() != null) {
            p.setJsonPericiasCustom(serializarCustom(req.periciasCustom()));
        }

        ResultadoCalculo r = calculoService.calcular(p);
        p.setAtributosFinais(r.atributosFinais());
        aplicarStatus(p, r.status());
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
        carregar(id);
        return auditoriaService.historico("Personagem", id).stream()
                .map(AuditoriaResponse::de).toList();
    }

    @Transactional
    public PersonagemResponse atualizarStatus(Long id, Integer pvAtual, Integer pmAtual, Integer peAtual) {
        return atualizarStatus(id, pvAtual, pmAtual, peAtual, null, null, null);
    }

    @Transactional
    public PersonagemResponse atualizarStatus(Long id, Integer pvAtual, Integer pmAtual, Integer peAtual,
                                              Integer pvMax, Integer pmMax, Integer peMax) {
        Personagem p = carregar(id);
        var status = p.getStatus();

        // Tetos manuais (override que persiste entre recalculos); valor <= 0 limpa o override.
        if (pvMax != null) {
            p.setPvMaxManual(pvMax > 0 ? pvMax : null);
            if (pvMax > 0) { status.setPvMax(pvMax); if (status.getPvAtual() > pvMax) status.setPvAtual(pvMax); }
        }
        if (pmMax != null) {
            p.setPmMaxManual(pmMax > 0 ? pmMax : null);
            if (pmMax > 0) { status.setPmMax(pmMax); if (status.getPmAtual() > pmMax) status.setPmAtual(pmMax); }
        }
        if (peMax != null) {
            p.setPeMaxManual(peMax > 0 ? peMax : null);
            if (peMax > 0) { status.setPeMax(peMax); if (status.getPeAtual() > peMax) status.setPeAtual(peMax); }
        }

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

    /** Aplica os tetos manuais (se houver) sobre o status recem-calculado e ajusta o atual ao teto. */
    private void aplicarStatus(Personagem p, Status novo) {
        if (p.getPvMaxManual() != null) novo.setPvMax(p.getPvMaxManual());
        if (p.getPmMaxManual() != null) novo.setPmMax(p.getPmMaxManual());
        if (p.getPeMaxManual() != null) novo.setPeMax(p.getPeMaxManual());
        Status antigo = p.getStatus();
        if (antigo != null) {
            novo.setPvAtual(Math.min(antigo.getPvAtual(), novo.getPvMax()));
            novo.setPmAtual(Math.min(antigo.getPmAtual(), novo.getPmMax()));
            novo.setPeAtual(Math.min(antigo.getPeAtual(), novo.getPeMax()));
        }
        p.setStatus(novo);
    }

    /** Define XP e/ou nivel, sobe de nivel automaticamente pelo XP e devolve os ganhos (popup). */
    @Transactional
    public ProgressoResponse atualizarProgresso(Long id, Integer xpAtual, Integer nivelAlvoReq) {
        Personagem p = carregar(id);
        int nivelAntes = p.getNivel();

        if (xpAtual != null) {
            p.setXpAtual(Math.max(0, xpAtual));
        }
        int nivelAlvo = (nivelAlvoReq != null) ? Math.max(0, nivelAlvoReq) : p.getNivel();
        nivelAlvo = Math.max(nivelAlvo, nivelPorXp(p.getXpAtual()));

        List<NivelGanho> ganhos = new ArrayList<>();
        if (nivelAlvo > nivelAntes) {
            Map<Integer, ProgressaoNivel> tabela = new HashMap<>();
            for (ProgressaoNivel pn : progressaoNivelRepository.findByGameSystemIdOrderByNivel(p.getGameSystemId())) {
                tabela.put(pn.getNivel(), pn);
            }
            for (int n = nivelAntes + 1; n <= nivelAlvo; n++) {
                ProgressaoNivel pn = tabela.get(n);
                ganhos.add(new NivelGanho(n,
                        pn != null ? pn.getRecompensa() : "Novo nivel",
                        pn != null ? pn.getLimiteAtributo() : 0));
            }
        }
        p.setNivel(nivelAlvo);

        ResultadoCalculo r = calculoService.calcular(p);
        p.setAtributosFinais(r.atributosFinais());
        aplicarStatus(p, r.status());
        p = personagemRepository.save(p);

        if (nivelAlvo != nivelAntes) {
            snapshotService.criar(p, "LEVEL_UP");
            auditoriaService.registrar(p.getOrganizacaoId(), p.getUsuarioId(), "PERSONAGEM_ATUALIZADO",
                    "Personagem", p.getId(), "nivel", String.valueOf(nivelAntes), String.valueOf(nivelAlvo));
        }
        realtimeNotifier.statusPersonagem(p.getId(), StatusDto.de(p.getStatus()));
        return new ProgressoResponse(toResponse(p), ganhos);
    }

    /** Maior nivel cuja exigencia de XP foi atingida (0 quando XP <= 0). */
    private int nivelPorXp(int xp) {
        int nivel = 1;
        for (ProgressaoNivel pn : progressaoNivelRepository.findByGameSystemIdOrderByNivel(asus().getId())) {
            if (xp >= pn.getXpNecessario()) {
                nivel = pn.getNivel();
            } else {
                break;
            }
        }
        return nivel;
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
                r.deslocamento(), r.cargaMaxima(),
                r.limiteHabilidades(), r.limiteFeiticos(), r.limiteBencaos(),
                r.pericias().stream().map(this::toPericiaDto).toList(),
                r.passos());
    }

    public List<SnapshotResponse> snapshots(Long id) {
        carregar(id);
        return snapshotService.listar(id).stream().map(SnapshotResponse::de).toList();
    }

    // ----- helpers -----

    private GameSystem asus() {
        return gameSystemRepository.findByCodigo(AsusV1Engine.SYSTEM_ID)
                .orElseThrow(() -> new NotFoundException("Sistema ASUS nao encontrado"));
    }

    private Personagem carregar(Long id) {
        return personagemRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Personagem " + id + " nao encontrado"));
    }

    private Personagem montarSalvar(Long organizacaoId, Long usuarioId, String nome, String jogador,
                                    String racaCodigo, String classeCodigo, String trilhaCodigo,
                                    String divindade, int nivel, int xpAtual,
                                    AtributosDto atributosBase, Map<String, Integer> pericias) {
        GameSystem asus = asus();

        Raca raca = racaRepository.findByGameSystemIdAndCodigo(asus.getId(), racaCodigo)
                .orElseThrow(() -> new NotFoundException("Raca '" + racaCodigo + "' nao encontrada"));

        Classe classe = classeRepository.findByGameSystemIdAndCodigo(asus.getId(), classeCodigo)
                .orElseThrow(() -> new NotFoundException("Classe '" + classeCodigo + "' nao encontrada"));

        Long trilhaId = null;
        if (trilhaCodigo != null && !trilhaCodigo.isBlank()) {
            trilhaId = classeRepository.findByGameSystemIdAndCodigo(asus.getId(), trilhaCodigo)
                    .map(Classe::getId)
                    .orElseThrow(() -> new NotFoundException("Trilha '" + trilhaCodigo + "' nao encontrada"));
        }

        Personagem p = Personagem.builder()
                .organizacaoId(organizacaoId)
                .usuarioId(usuarioId)
                .gameSystemId(asus.getId())
                .rulesetVersion(AsusV1Engine.VERSION)
                .nome(nome)
                .jogador(jogador)
                .racaId(raca.getId())
                .classeId(classe.getId())
                .trilhaId(trilhaId)
                .divindade(divindade)
                .jsonPericias(serializar(pericias))
                .nivel(nivel)
                .xpAtual(xpAtual)
                .atributosBase(atributosBase.paraEntidade())
                .arquivado(false)
                .build();

        ResultadoCalculo r = calculoService.calcular(p);
        p.setAtributosFinais(r.atributosFinais());
        p.setStatus(r.status());

        return personagemRepository.save(p);
    }

    private String serializar(Map<String, Integer> pericias) {
        if (pericias == null || pericias.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(pericias);
        } catch (Exception e) {
            return null;
        }
    }

    private String serializarCustom(List<PericiaCustomDto> lista) {
        if (lista == null || lista.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(lista);
        } catch (Exception e) {
            return null;
        }
    }

    /** Regra ASUS: na criacao (nivel 1) distribui-se 5 pontos de atributo e 5 de pericia,
     *  cada atributo limitado ao teto do nivel. */
    private void validarDistribuicaoCriacao(AtributosDto a, Map<String, Integer> pericias, int nivel,
                                            String classeCodigo, String trilhaCodigo) {
        if (a != null) {
            int soma = somaAtributos(a);
            if (soma > 5) {
                throw new IllegalArgumentException(
                        "Na criacao voce distribui no maximo 5 pontos de atributo (usou " + soma + ").");
            }
            validarTetoAtributo(a, nivel, classeCodigo, trilhaCodigo);
        }
        if (pericias != null) {
            int sp = pericias.values().stream().mapToInt(v -> v == null ? 0 : v).sum();
            if (sp > 5) {
                throw new IllegalArgumentException(
                        "Na criacao voce distribui no maximo 5 pontos de pericia (usou " + sp + ").");
            }
        }
    }

    /** O valor FINAL (pontos distribuidos + bonus fixo de classe/trilha) nao pode passar do teto do nivel. */
    private void validarTetoAtributo(AtributosDto a, int nivel, String classeCodigo, String trilhaCodigo) {
        int teto = tetoNivel(nivel);
        int[] base = aArray(a);
        int[] bonus = bonusFixosAtributo(classeCodigo, trilhaCodigo);
        String[] nomes = {"Forca", "Constituicao", "Destreza", "Agilidade", "Inteligencia", "Sabedoria", "Carisma"};
        for (int i = 0; i < 7; i++) {
            if (base[i] + bonus[i] > teto) {
                throw new IllegalArgumentException(nomes[i] + " passa do teto do nivel " + nivel + " (" + teto
                        + "): " + base[i] + " distribuido + " + bonus[i] + " fixo da classe = " + (base[i] + bonus[i]) + ".");
            }
        }
    }

    private int somaAtributos(AtributosDto a) {
        return a.forca() + a.constituicao() + a.destreza() + a.agilidade()
                + a.inteligencia() + a.sabedoria() + a.carisma();
    }

    private int[] aArray(AtributosDto a) {
        return new int[]{a.forca(), a.constituicao(), a.destreza(), a.agilidade(),
                a.inteligencia(), a.sabedoria(), a.carisma()};
    }

    /** Soma do bonus fixo de atributo da classe (e trilha), por ordinal de Atributo. */
    private int[] bonusFixosAtributo(String... codigos) {
        int[] b = new int[7];
        Long sid = asus().getId();
        for (String cod : codigos) {
            if (cod == null || cod.isBlank()) {
                continue;
            }
            var opt = classeRepository.findByGameSystemIdAndCodigo(sid, cod);
            if (opt.isEmpty() || opt.get().getJsonBonus() == null || opt.get().getJsonBonus().isBlank()) {
                continue;
            }
            try {
                com.fasterxml.jackson.databind.JsonNode atrs =
                        objectMapper.readTree(opt.get().getJsonBonus()).get("atributos");
                if (atrs != null && atrs.isObject()) {
                    var it = atrs.fields();
                    while (it.hasNext()) {
                        var e = it.next();
                        try {
                            b[Atributo.valueOf(e.getKey().toUpperCase(Locale.ROOT)).ordinal()] += e.getValue().asInt();
                        } catch (Exception ignored) {
                            // chave que nao e atributo conhecido
                        }
                    }
                }
            } catch (Exception ignored) {
                // json invalido
            }
        }
        return b;
    }

    private int tetoNivel(int nivel) {
        int teto = 99;
        for (ProgressaoNivel pn : progressaoNivelRepository.findByGameSystemIdOrderByNivel(asus().getId())) {
            if (pn.getNivel() == nivel) {
                teto = pn.getLimiteAtributo();
                break;
            }
        }
        return teto;
    }

    private PersonagemResponse toResponse(Personagem p) {
        GameSystem sistema = gameSystemRepository.findById(p.getGameSystemId()).orElse(null);
        Raca raca = p.getRacaId() == null ? null : racaRepository.findById(p.getRacaId()).orElse(null);
        Classe classe = p.getClasseId() == null ? null : classeRepository.findById(p.getClasseId()).orElse(null);
        Classe trilha = p.getTrilhaId() == null ? null : classeRepository.findById(p.getTrilhaId()).orElse(null);

        ResultadoCalculo r = calculoService.calcular(p);
        List<PericiaCalculadaDto> pericias = new ArrayList<>(r.pericias().stream().map(this::toPericiaDto).toList());
        // Perícias "Outros" (concedidas por itens), com teto = 2x atributo final.
        if (p.getJsonPericiasCustom() != null && !p.getJsonPericiasCustom().isBlank()) {
            try {
                com.fasterxml.jackson.databind.JsonNode arr = objectMapper.readTree(p.getJsonPericiasCustom());
                if (arr.isArray()) {
                    for (com.fasterxml.jackson.databind.JsonNode node : arr) {
                        String nome = node.path("nome").asText("");
                        if (nome.isBlank()) {
                            continue;
                        }
                        Atributo at;
                        try {
                            at = Atributo.valueOf(node.path("atributo").asText("FORCA").toUpperCase(Locale.ROOT));
                        } catch (Exception e) {
                            at = Atributo.FORCA;
                        }
                        int finalAttr = p.getAtributosFinais() == null ? 0 : p.getAtributosFinais().get(at);
                        int cap = Math.max(0, finalAttr * 2);
                        int treino = Math.max(0, Math.min(node.path("treino").asInt(0), cap));
                        pericias.add(new PericiaCalculadaDto("OUTROS:" + nome, nome, at.name(), at.getSigla(), treino, cap, true));
                    }
                }
            } catch (Exception ignored) {
                // json invalido: ignora
            }
        }

        // Teto de atributo do nivel atual e XP para o proximo nivel (ex.: "0/100 para o nivel 2").
        int limiteAtributo = 0;
        Integer xpProximoNivel = null;
        for (ProgressaoNivel pn : progressaoNivelRepository.findByGameSystemIdOrderByNivel(p.getGameSystemId())) {
            if (pn.getNivel() == p.getNivel()) {
                limiteAtributo = pn.getLimiteAtributo();
            }
            if (pn.getNivel() == p.getNivel() + 1) {
                xpProximoNivel = pn.getXpNecessario();
            }
        }

        // Carga atual = soma de espacos x quantidade do inventario (max = Forca x 2).
        int cargaAtual = 0;
        for (ItemPersonagem it : itemPersonagemRepository.findByPersonagemId(p.getId())) {
            int esp = it.getEspacos() == null ? 0 : it.getEspacos();
            int qtd = it.getQuantidade() == null ? 1 : it.getQuantidade();
            cargaAtual += esp * qtd;
        }
        // Sobrecarga: acima da carga maxima, o deslocamento cai pela metade.
        boolean sobrecarregado = cargaAtual > r.cargaMaxima();
        int deslocamentoFinal = sobrecarregado ? Math.max(1, r.deslocamento() / 2) : r.deslocamento();

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
                trilha == null ? null : trilha.getCodigo(),
                trilha == null ? null : trilha.getNome(),
                p.getDivindade(),
                p.getAvatarAssetId(),
                p.getNivel(),
                p.getXpAtual(),
                AtributosDto.de(p.getAtributosBase()),
                AtributosDto.de(p.getAtributosFinais()),
                StatusDto.de(p.getStatus()),
                deslocamentoFinal,
                r.cargaMaxima(),
                cargaAtual,
                r.limiteHabilidades(),
                r.limiteFeiticos(),
                r.limiteBencaos(),
                limiteAtributo,
                xpProximoNivel,
                pericias,
                p.getAnotacoes(),
                p.getAparencia(),
                p.getPersonalidade(),
                p.getHistorico(),
                p.getObjetivo(),
                p.isArquivado(),
                p.getCriadoEm(),
                p.getAtualizadoEm());
    }

    private PericiaCalculadaDto toPericiaDto(PericiaCalculada pc) {
        return new PericiaCalculadaDto(pc.codigo(), pc.nome(), pc.atributoBase(),
                pc.sigla(), pc.treino(), pc.cap(), false);
    }
}
