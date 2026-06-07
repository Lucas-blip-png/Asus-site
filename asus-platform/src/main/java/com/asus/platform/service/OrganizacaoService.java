package com.asus.platform.service;

import com.asus.platform.domain.Organizacao;
import com.asus.platform.domain.OrganizacaoMembro;
import com.asus.platform.domain.PapelOrganizacao;
import com.asus.platform.domain.Plano;
import com.asus.platform.repository.OrganizacaoMembroRepository;
import com.asus.platform.repository.OrganizacaoRepository;
import com.asus.platform.repository.UsuarioRepository;
import com.asus.platform.web.NotFoundException;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrganizacaoService {

    private final OrganizacaoRepository repository;
    private final OrganizacaoMembroRepository membroRepository;
    private final UsuarioRepository usuarioRepository;
    private final AuditoriaService auditoriaService;

    public OrganizacaoService(OrganizacaoRepository repository,
                              OrganizacaoMembroRepository membroRepository,
                              UsuarioRepository usuarioRepository,
                              AuditoriaService auditoriaService) {
        this.repository = repository;
        this.membroRepository = membroRepository;
        this.usuarioRepository = usuarioRepository;
        this.auditoriaService = auditoriaService;
    }

    public List<Organizacao> listar() {
        return repository.findAll();
    }

    public Organizacao buscar(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new NotFoundException("Organizacao " + id + " nao encontrada"));
    }

    @Transactional
    public Organizacao criar(String nome, String slug) {
        if (repository.existsBySlug(slug)) {
            throw new IllegalArgumentException("Ja existe organizacao com o slug '" + slug + "'");
        }
        Organizacao org = repository.save(Organizacao.builder()
                .nome(nome).slug(slug).plano(Plano.FREE).build());
        auditoriaService.registrar(org.getId(), null, "ORGANIZACAO_CRIADA",
                "Organizacao", org.getId(), null, null, nome);
        return org;
    }

    @Transactional
    public Organizacao atualizar(Long id, String nome) {
        Organizacao org = buscar(id);
        if (nome != null) {
            org.setNome(nome);
        }
        org = repository.save(org);
        auditoriaService.registrar(id, null, "ORGANIZACAO_ATUALIZADA",
                "Organizacao", id, null, null, org.getNome());
        return org;
    }

    public List<OrganizacaoMembro> listarMembros(Long organizacaoId) {
        buscar(organizacaoId);
        return membroRepository.findByOrganizacaoId(organizacaoId);
    }

    @Transactional
    public OrganizacaoMembro adicionarMembro(Long organizacaoId, Long usuarioId, PapelOrganizacao papel) {
        buscar(organizacaoId);
        if (!usuarioRepository.existsById(usuarioId)) {
            throw new NotFoundException("Usuario " + usuarioId + " nao encontrado");
        }
        Optional<OrganizacaoMembro> existente = membroRepository.findByOrganizacaoId(organizacaoId)
                .stream().filter(m -> m.getUsuarioId().equals(usuarioId)).findFirst();
        OrganizacaoMembro membro = existente.orElseGet(() ->
                OrganizacaoMembro.builder().organizacaoId(organizacaoId).usuarioId(usuarioId).build());
        membro.setPapel(papel == null ? PapelOrganizacao.JOGADOR : papel);
        membro = membroRepository.save(membro);
        auditoriaService.registrar(organizacaoId, usuarioId, "MEMBRO_ADICIONADO",
                "Organizacao", organizacaoId, "papel", null, membro.getPapel().name());
        return membro;
    }

    @Transactional
    public void removerMembro(Long organizacaoId, Long usuarioId) {
        List<OrganizacaoMembro> alvo = membroRepository.findByOrganizacaoId(organizacaoId)
                .stream().filter(m -> m.getUsuarioId().equals(usuarioId)).toList();
        if (alvo.isEmpty()) {
            throw new NotFoundException("Usuario " + usuarioId + " nao e membro da organizacao");
        }
        membroRepository.deleteAll(alvo);
        auditoriaService.registrar(organizacaoId, usuarioId, "MEMBRO_REMOVIDO",
                "Organizacao", organizacaoId, null, null, null);
    }
}
