package com.asus.platform.service;

import com.asus.platform.domain.GameSystem;
import com.asus.platform.domain.Template;
import com.asus.platform.domain.TipoTemplate;
import com.asus.platform.engine.AsusV1Engine;
import com.asus.platform.repository.GameSystemRepository;
import com.asus.platform.repository.TemplateRepository;
import com.asus.platform.web.NotFoundException;
import com.asus.platform.web.dto.CriarTemplateRequest;
import com.asus.platform.web.dto.TemplateResponse;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Templates reutilizaveis (plano, Seção 15 / Fase 12). */
@Service
public class TemplateService {

    private final TemplateRepository templateRepository;
    private final OrganizacaoService organizacaoService;
    private final GameSystemRepository gameSystemRepository;
    private final AuditoriaService auditoriaService;

    public TemplateService(TemplateRepository templateRepository,
                           OrganizacaoService organizacaoService,
                           GameSystemRepository gameSystemRepository,
                           AuditoriaService auditoriaService) {
        this.templateRepository = templateRepository;
        this.organizacaoService = organizacaoService;
        this.gameSystemRepository = gameSystemRepository;
        this.auditoriaService = auditoriaService;
    }

    public List<TemplateResponse> listar(Long organizacaoId) {
        organizacaoService.buscar(organizacaoId); // valida existencia
        return templateRepository.findByOrganizacaoId(organizacaoId).stream()
                .map(TemplateResponse::de).toList();
    }

    public List<TemplateResponse> listarPublicos() {
        return templateRepository.findByPublicoTrueOrderByCriadoEmDesc().stream()
                .map(TemplateResponse::de).toList();
    }

    public TemplateResponse buscar(Long id) {
        return TemplateResponse.de(carregar(id));
    }

    @Transactional
    public TemplateResponse criar(Long organizacaoId, CriarTemplateRequest req) {
        organizacaoService.buscar(organizacaoId); // valida existencia
        GameSystem asus = gameSystemRepository.findByCodigo(AsusV1Engine.SYSTEM_ID)
                .orElseThrow(() -> new NotFoundException("Sistema ASUS nao encontrado"));

        Template template = templateRepository.save(Template.builder()
                .organizacaoId(organizacaoId)
                .gameSystemId(asus.getId())
                .autorUsuarioId(req.autorUsuarioId())
                .tipo(TipoTemplate.deOuOutro(req.tipo()).name())
                .nome(req.nome())
                .descricao(req.descricao())
                .jsonConteudo(req.jsonConteudo())
                .oficial(false)
                .publico(req.publico() != null && req.publico())
                .build());

        auditoriaService.registrar(organizacaoId, req.autorUsuarioId(), "TEMPLATE_CRIADO",
                "Template", template.getId(), null, null, template.getNome());

        return TemplateResponse.de(template);
    }

    @Transactional
    public void apagar(Long id) {
        Template template = carregar(id);
        templateRepository.delete(template);
        auditoriaService.registrar(template.getOrganizacaoId(), template.getAutorUsuarioId(),
                "TEMPLATE_REMOVIDO", "Template", id, null, template.getNome(), null);
    }

    private Template carregar(Long id) {
        return templateRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Template " + id + " nao encontrado"));
    }
}
