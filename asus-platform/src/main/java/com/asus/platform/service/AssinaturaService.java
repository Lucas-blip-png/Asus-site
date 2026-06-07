package com.asus.platform.service;

import com.asus.platform.domain.Assinatura;
import com.asus.platform.domain.Organizacao;
import com.asus.platform.domain.Plano;
import com.asus.platform.repository.AssinaturaRepository;
import com.asus.platform.repository.OrganizacaoRepository;
import com.asus.platform.web.NotFoundException;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Assinatura/plano manual no banco (plano, Seção 4.4 — gateway fica para depois).
 */
@Service
public class AssinaturaService {

    private final AssinaturaRepository assinaturaRepository;
    private final OrganizacaoRepository organizacaoRepository;
    private final AuditoriaService auditoriaService;

    public AssinaturaService(AssinaturaRepository assinaturaRepository,
                             OrganizacaoRepository organizacaoRepository,
                             AuditoriaService auditoriaService) {
        this.assinaturaRepository = assinaturaRepository;
        this.organizacaoRepository = organizacaoRepository;
        this.auditoriaService = auditoriaService;
    }

    /** Assinatura atual da organizacao (null se nunca teve assinatura formal). */
    public Assinatura atual(Long organizacaoId) {
        buscarOrg(organizacaoId);
        return assinaturaRepository.findByOrganizacaoId(organizacaoId).orElse(null);
    }

    @Transactional
    public Assinatura definirPlano(Long organizacaoId, Plano plano) {
        Organizacao org = buscarOrg(organizacaoId);
        Plano anterior = org.getPlano();
        org.setPlano(plano);
        organizacaoRepository.save(org);

        Assinatura assinatura = assinaturaRepository.findByOrganizacaoId(organizacaoId)
                .orElseGet(() -> Assinatura.builder().organizacaoId(organizacaoId).build());
        assinatura.setPlano(plano);
        assinatura.setStatus("ATIVA");
        if (assinatura.getInicio() == null) {
            assinatura.setInicio(LocalDateTime.now());
        }
        assinatura = assinaturaRepository.save(assinatura);

        auditoriaService.registrar(organizacaoId, null, "PLANO_ALTERADO",
                "Organizacao", organizacaoId, "plano",
                anterior == null ? null : anterior.name(), plano.name());

        return assinatura;
    }

    private Organizacao buscarOrg(Long organizacaoId) {
        return organizacaoRepository.findById(organizacaoId)
                .orElseThrow(() -> new NotFoundException("Organizacao " + organizacaoId + " nao encontrada"));
    }
}
