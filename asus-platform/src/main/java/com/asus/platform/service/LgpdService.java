package com.asus.platform.service;

import com.asus.platform.domain.Consentimento;
import com.asus.platform.domain.Usuario;
import com.asus.platform.repository.CampanhaMembroRepository;
import com.asus.platform.repository.CompraRepository;
import com.asus.platform.repository.ConsentimentoRepository;
import com.asus.platform.repository.OrganizacaoMembroRepository;
import com.asus.platform.repository.PersonagemRepository;
import com.asus.platform.repository.UsuarioRepository;
import com.asus.platform.web.NotFoundException;
import com.asus.platform.web.dto.ConsentimentoResponse;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Direitos do titular: exportar dados, consentimentos e exclusao/anonimizacao (Seção 19 / Fase 13). */
@Service
public class LgpdService {

    private final UsuarioRepository usuarioRepository;
    private final ConsentimentoRepository consentimentoRepository;
    private final PersonagemRepository personagemRepository;
    private final OrganizacaoMembroRepository organizacaoMembroRepository;
    private final CampanhaMembroRepository campanhaMembroRepository;
    private final CompraRepository compraRepository;
    private final AuditoriaService auditoriaService;

    public LgpdService(UsuarioRepository usuarioRepository,
                       ConsentimentoRepository consentimentoRepository,
                       PersonagemRepository personagemRepository,
                       OrganizacaoMembroRepository organizacaoMembroRepository,
                       CampanhaMembroRepository campanhaMembroRepository,
                       CompraRepository compraRepository,
                       AuditoriaService auditoriaService) {
        this.usuarioRepository = usuarioRepository;
        this.consentimentoRepository = consentimentoRepository;
        this.personagemRepository = personagemRepository;
        this.organizacaoMembroRepository = organizacaoMembroRepository;
        this.campanhaMembroRepository = campanhaMembroRepository;
        this.compraRepository = compraRepository;
        this.auditoriaService = auditoriaService;
    }

    /** Pacote com todos os dados do usuario (portabilidade — Seção 19.1). */
    public Map<String, Object> exportarDados(Long usuarioId) {
        Usuario usuario = usuario(usuarioId);
        Map<String, Object> dados = new LinkedHashMap<>();
        dados.put("usuario", usuario);
        dados.put("consentimentos", consentimentoRepository.findByUsuarioIdOrderByCriadoEmDesc(usuarioId));
        dados.put("personagens", personagemRepository.findByUsuarioId(usuarioId));
        dados.put("organizacoes", organizacaoMembroRepository.findByUsuarioId(usuarioId));
        dados.put("campanhas", campanhaMembroRepository.findByUsuarioId(usuarioId));
        dados.put("compras", compraRepository.findByUsuarioIdOrderByCompradoEmDesc(usuarioId));
        auditoriaService.registrar(null, usuarioId, "DADOS_EXPORTADOS",
                "Usuario", usuarioId, null, null, null);
        return dados;
    }

    @Transactional
    public ConsentimentoResponse registrarConsentimento(Long usuarioId, String tipo,
                                                        String versaoDocumento, boolean aceito) {
        usuario(usuarioId);
        Consentimento consentimento = consentimentoRepository.save(Consentimento.builder()
                .usuarioId(usuarioId)
                .tipo(tipo)
                .versaoDocumento(versaoDocumento)
                .aceito(aceito)
                .build());
        auditoriaService.registrar(null, usuarioId, "CONSENTIMENTO_REGISTRADO",
                "Consentimento", consentimento.getId(), tipo, null, String.valueOf(aceito));
        return ConsentimentoResponse.de(consentimento);
    }

    /** Anonimiza os dados pessoais, preservando integridade referencial (Seção 19.1). */
    @Transactional
    public void excluirConta(Long usuarioId) {
        Usuario usuario = usuario(usuarioId);
        usuario.setNome("Usuario removido");
        usuario.setEmail("removido+" + usuarioId + "@anonimizado.local");
        usuario.setAnonimizado(true);
        usuarioRepository.save(usuario);
        auditoriaService.registrar(null, usuarioId, "CONTA_ANONIMIZADA",
                "Usuario", usuarioId, null, null, null);
    }

    private Usuario usuario(Long id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Usuario " + id + " nao encontrado"));
    }
}
