package com.asus.platform.service;

import com.asus.platform.domain.Campanha;
import com.asus.platform.domain.CampanhaMembro;
import com.asus.platform.domain.CampanhaPersonagem;
import com.asus.platform.domain.Convite;
import com.asus.platform.domain.GameSystem;
import com.asus.platform.domain.Organizacao;
import com.asus.platform.domain.PapelCampanha;
import com.asus.platform.domain.Permissao;
import com.asus.platform.domain.Permissoes;
import com.asus.platform.domain.Personagem;
import com.asus.platform.engine.AsusV1Engine;
import com.asus.platform.domain.Classe;
import com.asus.platform.repository.CampanhaMembroRepository;
import com.asus.platform.repository.CampanhaPersonagemRepository;
import com.asus.platform.repository.CampanhaRepository;
import com.asus.platform.repository.ClasseRepository;
import com.asus.platform.repository.CombateParticipanteRepository;
import com.asus.platform.repository.CombateRepository;
import com.asus.platform.repository.ConviteRepository;
import com.asus.platform.repository.GameSystemRepository;
import com.asus.platform.repository.PersonagemRepository;
import com.asus.platform.repository.UsuarioRepository;
import com.asus.platform.web.NotFoundException;
import com.asus.platform.web.dto.AtualizarCampanhaRequest;
import com.asus.platform.web.dto.CampanhaMembroResponse;
import com.asus.platform.web.dto.CampanhaPersonagemResponse;
import com.asus.platform.web.dto.CampanhaResponse;
import com.asus.platform.web.dto.ConviteResponse;
import com.asus.platform.web.dto.CriarCampanhaRequest;
import com.asus.platform.web.dto.CriarConviteRequest;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Campanhas, membros, vinculo de personagens e convites (plano, secoes 8, 9 e 21.4). */
@Service
public class CampanhaService {

    private static final String ALFABETO = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final int TAMANHO_CODIGO = 8;
    private final SecureRandom random = new SecureRandom();

    private final CampanhaRepository campanhaRepository;
    private final CampanhaPersonagemRepository campanhaPersonagemRepository;
    private final CampanhaMembroRepository campanhaMembroRepository;
    private final ConviteRepository conviteRepository;
    private final OrganizacaoService organizacaoService;
    private final GameSystemRepository gameSystemRepository;
    private final PersonagemRepository personagemRepository;
    private final ClasseRepository classeRepository;
    private final UsuarioRepository usuarioRepository;
    private final CombateRepository combateRepository;
    private final CombateParticipanteRepository combateParticipanteRepository;
    private final AuditoriaService auditoriaService;
    private final PlanoService planoService;

    public CampanhaService(CampanhaRepository campanhaRepository,
                           CampanhaPersonagemRepository campanhaPersonagemRepository,
                           CampanhaMembroRepository campanhaMembroRepository,
                           ConviteRepository conviteRepository,
                           OrganizacaoService organizacaoService,
                           GameSystemRepository gameSystemRepository,
                           PersonagemRepository personagemRepository,
                           ClasseRepository classeRepository,
                           UsuarioRepository usuarioRepository,
                           CombateRepository combateRepository,
                           CombateParticipanteRepository combateParticipanteRepository,
                           AuditoriaService auditoriaService,
                           PlanoService planoService) {
        this.campanhaRepository = campanhaRepository;
        this.campanhaPersonagemRepository = campanhaPersonagemRepository;
        this.campanhaMembroRepository = campanhaMembroRepository;
        this.conviteRepository = conviteRepository;
        this.organizacaoService = organizacaoService;
        this.gameSystemRepository = gameSystemRepository;
        this.personagemRepository = personagemRepository;
        this.classeRepository = classeRepository;
        this.usuarioRepository = usuarioRepository;
        this.combateRepository = combateRepository;
        this.combateParticipanteRepository = combateParticipanteRepository;
        this.auditoriaService = auditoriaService;
        this.planoService = planoService;
    }

    public List<CampanhaResponse> listar(Long organizacaoId) {
        organizacaoService.buscar(organizacaoId); // valida existencia
        String systemId = asus().getCodigo();
        return campanhaRepository.findByOrganizacaoIdAndArquivadaFalse(organizacaoId)
                .stream().map(c -> CampanhaResponse.de(c, systemId)).toList();
    }

    /** Campanhas das quais o usuario e membro (mestre ou jogador), de qualquer organizacao. */
    public List<CampanhaResponse> listarDoUsuario(Long usuarioId) {
        String systemId = asus().getCodigo();
        return campanhaMembroRepository.findByUsuarioId(usuarioId).stream()
                .map(m -> campanhaRepository.findById(m.getCampanhaId()).orElse(null))
                .filter(c -> c != null && !c.isArquivada())
                .map(c -> CampanhaResponse.de(c, systemId))
                .toList();
    }

    public CampanhaResponse buscar(Long id) {
        Campanha campanha = carregar(id);
        return CampanhaResponse.de(campanha, systemIdDe(campanha));
    }

    @Transactional
    public CampanhaResponse criar(Long organizacaoId, CriarCampanhaRequest req) {
        Organizacao org = organizacaoService.buscar(organizacaoId);
        planoService.validarNovaCampanha(organizacaoId); // Fase 10: limite do plano
        GameSystem asus = asus();

        Long mestreId = req.mestreId() != null ? req.mestreId() : org.getDonoId();

        Campanha campanha = Campanha.builder()
                .organizacaoId(organizacaoId)
                .mestreId(mestreId)
                .gameSystemId(asus.getId())
                .nome(req.nome())
                .descricao(com.asus.platform.util.Sanitizador.limpar(req.descricao()))
                .config(req.config() == null ? null : req.config().paraEntidade())
                .arquivada(false)
                .build();
        campanha = campanhaRepository.save(campanha);

        // O mestre ja entra como membro MESTRE.
        if (mestreId != null) {
            campanhaMembroRepository.save(CampanhaMembro.builder()
                    .campanhaId(campanha.getId())
                    .usuarioId(mestreId)
                    .papel(PapelCampanha.MESTRE)
                    .build());
        }

        auditoriaService.registrar(organizacaoId, mestreId, "CAMPANHA_CRIADA",
                "Campanha", campanha.getId(), null, null, campanha.getNome());

        return CampanhaResponse.de(campanha, asus.getCodigo());
    }

    @Transactional
    public CampanhaResponse atualizar(Long id, AtualizarCampanhaRequest req) {
        Campanha campanha = carregar(id);
        if (req.nome() != null) {
            campanha.setNome(req.nome());
        }
        if (req.descricao() != null) {
            campanha.setDescricao(com.asus.platform.util.Sanitizador.limpar(req.descricao()));
        }
        if (req.capaAssetId() != null) {
            campanha.setCapaAssetId(req.capaAssetId());
        }
        if (req.arquivada() != null) {
            campanha.setArquivada(req.arquivada());
        }
        if (req.config() != null) {
            campanha.setConfig(req.config().paraEntidade());
        }
        campanha = campanhaRepository.save(campanha);

        auditoriaService.registrar(campanha.getOrganizacaoId(), campanha.getMestreId(),
                "CAMPANHA_ATUALIZADA", "Campanha", campanha.getId(), null, null, campanha.getNome());

        return CampanhaResponse.de(campanha, systemIdDe(campanha));
    }

    /** Anotações do mestre (privadas): só quem gerencia a campanha pode ler. */
    public String obterAnotacoes(Long campanhaId, Long usuarioId) {
        Campanha campanha = carregar(campanhaId);
        exigirPermissao(campanhaId, usuarioId, Permissao.GERENCIAR_CAMPANHA);
        return campanha.getAnotacoes();
    }

    @Transactional
    public String salvarAnotacoes(Long campanhaId, Long usuarioId, String texto) {
        Campanha campanha = carregar(campanhaId);
        exigirPermissao(campanhaId, usuarioId, Permissao.GERENCIAR_CAMPANHA);
        campanha.setAnotacoes(texto == null ? null : com.asus.platform.util.Sanitizador.limpar(texto));
        campanhaRepository.save(campanha);
        return campanha.getAnotacoes();
    }

    /** Apaga a campanha e seus vinculos (membros, personagens e combates). */
    @Transactional
    public void apagar(Long id) {
        Campanha campanha = carregar(id);
        campanhaMembroRepository.deleteAll(campanhaMembroRepository.findByCampanhaId(id));
        campanhaPersonagemRepository.deleteAll(campanhaPersonagemRepository.findByCampanhaId(id));
        combateRepository.findByCampanhaIdOrderByCriadoEmDesc(id).forEach(combate ->
                combateParticipanteRepository.deleteAll(
                        combateParticipanteRepository.findByCombateIdOrderByIniciativaDescIdAsc(combate.getId())));
        combateRepository.deleteAll(combateRepository.findByCampanhaIdOrderByCriadoEmDesc(id));
        campanhaRepository.delete(campanha);
        auditoriaService.registrar(campanha.getOrganizacaoId(), campanha.getMestreId(),
                "CAMPANHA_APAGADA", "Campanha", id, null, campanha.getNome(), null);
    }

    public List<CampanhaPersonagemResponse> listarPersonagens(Long campanhaId) {
        carregar(campanhaId); // valida existencia
        return campanhaPersonagemRepository.findByCampanhaId(campanhaId).stream()
                .map(cp -> {
                    Personagem p = personagemRepository.findById(cp.getPersonagemId()).orElse(null);
                    String classe = (p != null && p.getClasseId() != null)
                            ? classeRepository.findById(p.getClasseId()).map(Classe::getNome).orElse(null)
                            : null;
                    return CampanhaPersonagemResponse.de(cp, p, classe);
                })
                .toList();
    }

    @Transactional
    public CampanhaPersonagemResponse adicionarPersonagem(Long campanhaId, Long personagemId) {
        Campanha campanha = carregar(campanhaId);
        Personagem personagem = personagemRepository.findById(personagemId)
                .orElseThrow(() -> new NotFoundException("Personagem " + personagemId + " nao encontrado"));

        if (!personagem.getOrganizacaoId().equals(campanha.getOrganizacaoId())) {
            throw new IllegalArgumentException(
                    "Personagem e campanha pertencem a organizacoes diferentes");
        }
        if (campanhaPersonagemRepository.existsByCampanhaIdAndPersonagemId(campanhaId, personagemId)) {
            throw new IllegalArgumentException("Personagem ja esta nesta campanha");
        }

        CampanhaPersonagem cp = campanhaPersonagemRepository.save(CampanhaPersonagem.builder()
                .campanhaId(campanhaId)
                .personagemId(personagemId)
                .build());

        auditoriaService.registrar(campanha.getOrganizacaoId(), personagem.getUsuarioId(),
                "PERSONAGEM_ADICIONADO_CAMPANHA", "Campanha", campanhaId,
                "personagemId", null, String.valueOf(personagemId));

        String classe = personagem.getClasseId() == null ? null
                : classeRepository.findById(personagem.getClasseId()).map(Classe::getNome).orElse(null);
        return CampanhaPersonagemResponse.de(cp, personagem, classe);
    }

    public List<CampanhaMembroResponse> listarMembros(Long campanhaId) {
        carregar(campanhaId); // valida existencia
        return campanhaMembroRepository.findByCampanhaId(campanhaId).stream()
                .map(CampanhaMembroResponse::de).toList();
    }

    @Transactional
    public ConviteResponse criarConvite(Long campanhaId, CriarConviteRequest req) {
        Campanha campanha = carregar(campanhaId);

        // Quando o autor e informado, exige permissao de convidar (plano, secao 9).
        if (req.usuarioId() != null) {
            exigirPermissao(campanhaId, req.usuarioId(), Permissao.CONVIDAR_JOGADORES);
        }

        PapelCampanha papel = req.papel() != null ? req.papel() : PapelCampanha.JOGADOR;
        LocalDateTime expiraEm = req.expiraEmDias() != null
                ? LocalDateTime.now().plusDays(req.expiraEmDias())
                : null;

        Convite convite = Convite.builder()
                .campanhaId(campanhaId)
                .codigo(gerarCodigoUnico())
                .papel(papel)
                .criadoPorUsuarioId(req.usuarioId())
                .maxUsos(req.maxUsos())
                .usos(0)
                .expiraEm(expiraEm)
                .ativo(true)
                .build();
        convite = conviteRepository.save(convite);

        auditoriaService.registrar(campanha.getOrganizacaoId(), req.usuarioId(), "CONVITE_CRIADO",
                "Campanha", campanhaId, "codigo", null, convite.getCodigo());

        return ConviteResponse.de(convite);
    }

    @Transactional
    public CampanhaMembroResponse entrar(String codigo, Long usuarioId) {
        Convite convite = conviteRepository.findByCodigo(codigo)
                .orElseThrow(() -> new NotFoundException("Convite '" + codigo + "' nao encontrado"));

        if (!convite.utilizavel(LocalDateTime.now())) {
            throw new IllegalArgumentException("Convite expirado ou indisponivel");
        }
        if (!usuarioRepository.existsById(usuarioId)) {
            throw new NotFoundException("Usuario " + usuarioId + " nao encontrado");
        }

        Campanha campanha = carregar(convite.getCampanhaId());

        // Idempotente: ja membro -> devolve o vinculo existente sem consumir o convite.
        Optional<CampanhaMembro> existente =
                campanhaMembroRepository.findByCampanhaIdAndUsuarioId(campanha.getId(), usuarioId);
        if (existente.isPresent()) {
            return CampanhaMembroResponse.de(existente.get());
        }

        // Fase 10: limite de jogadores por campanha (so conta papel JOGADOR).
        if (convite.getPapel() == PapelCampanha.JOGADOR) {
            planoService.validarNovoJogador(campanha.getId(), campanha.getOrganizacaoId());
        }

        CampanhaMembro membro = campanhaMembroRepository.save(CampanhaMembro.builder()
                .campanhaId(campanha.getId())
                .usuarioId(usuarioId)
                .papel(convite.getPapel())
                .build());

        convite.setUsos(convite.getUsos() + 1);
        conviteRepository.save(convite);

        auditoriaService.registrar(campanha.getOrganizacaoId(), usuarioId, "ENTROU_CAMPANHA",
                "Campanha", campanha.getId(), "papel", null, convite.getPapel().name());

        return CampanhaMembroResponse.de(membro);
    }

    /** Papel de um usuario na campanha, se for membro. */
    public Optional<PapelCampanha> papelDe(Long campanhaId, Long usuarioId) {
        return campanhaMembroRepository.findByCampanhaIdAndUsuarioId(campanhaId, usuarioId)
                .map(CampanhaMembro::getPapel);
    }

    /** Lanca 403 (mapeado de IllegalState) quando o usuario nao tem a permissao. */
    public void exigirPermissao(Long campanhaId, Long usuarioId, Permissao permissao) {
        PapelCampanha papel = papelDe(campanhaId, usuarioId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Usuario " + usuarioId + " nao e membro da campanha " + campanhaId));
        if (!Permissoes.pode(papel, permissao)) {
            throw new IllegalArgumentException(
                    "Papel " + papel + " nao tem a permissao " + permissao);
        }
    }

    // ----- helpers -----

    Campanha carregar(Long id) {
        return campanhaRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Campanha " + id + " nao encontrada"));
    }

    private GameSystem asus() {
        return gameSystemRepository.findByCodigo(AsusV1Engine.SYSTEM_ID)
                .orElseThrow(() -> new NotFoundException("Sistema ASUS nao encontrado"));
    }

    private String systemIdDe(Campanha c) {
        return gameSystemRepository.findById(c.getGameSystemId())
                .map(GameSystem::getCodigo).orElse(AsusV1Engine.SYSTEM_ID);
    }

    private String gerarCodigoUnico() {
        for (int tentativa = 0; tentativa < 20; tentativa++) {
            StringBuilder sb = new StringBuilder(TAMANHO_CODIGO);
            for (int i = 0; i < TAMANHO_CODIGO; i++) {
                sb.append(ALFABETO.charAt(random.nextInt(ALFABETO.length())));
            }
            String codigo = sb.toString();
            if (!conviteRepository.existsByCodigo(codigo)) {
                return codigo;
            }
        }
        throw new IllegalStateException("Nao foi possivel gerar codigo de convite unico");
    }
}
