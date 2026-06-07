package com.asus.platform.service;

import com.asus.platform.domain.Organizacao;
import com.asus.platform.domain.Plano;
import com.asus.platform.repository.OrganizacaoRepository;
import com.asus.platform.web.NotFoundException;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrganizacaoService {

    private final OrganizacaoRepository repository;
    private final AuditoriaService auditoriaService;

    public OrganizacaoService(OrganizacaoRepository repository, AuditoriaService auditoriaService) {
        this.repository = repository;
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
        Organizacao org = Organizacao.builder()
                .nome(nome)
                .slug(slug)
                .plano(Plano.FREE)
                .build();
        org = repository.save(org);
        auditoriaService.registrar(org.getId(), null, "ORGANIZACAO_CRIADA",
                "Organizacao", org.getId(), null, null, nome);
        return org;
    }
}
