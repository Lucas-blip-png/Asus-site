package com.asus.platform.service;

import com.asus.platform.domain.Campanha;
import com.asus.platform.domain.Permissao;
import com.asus.platform.domain.Rolagem;
import com.asus.platform.engine.AsusV1Engine;
import com.asus.platform.engine.Dado;
import com.asus.platform.engine.ExpressaoDado;
import com.asus.platform.engine.ResultadoDado;
import com.asus.platform.realtime.RealtimeNotifier;
import com.asus.platform.repository.RolagemRepository;
import com.asus.platform.web.NotFoundException;
import com.asus.platform.web.dto.RolagemResponse;
import com.asus.platform.web.dto.RolarRequest;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Rolagens de dados, com critico/falha, ocultas e revelacao (plano, secao 21.5). */
@Service
public class RolagemService {

    private final RolagemRepository rolagemRepository;
    private final CampanhaService campanhaService;
    private final Dado dado;
    private final AuditoriaService auditoriaService;
    private final RealtimeNotifier realtimeNotifier;
    private final com.asus.platform.repository.PersonagemRepository personagemRepository;

    public RolagemService(RolagemRepository rolagemRepository,
                          CampanhaService campanhaService,
                          Dado dado,
                          AuditoriaService auditoriaService,
                          RealtimeNotifier realtimeNotifier,
                          com.asus.platform.repository.PersonagemRepository personagemRepository) {
        this.rolagemRepository = rolagemRepository;
        this.campanhaService = campanhaService;
        this.dado = dado;
        this.auditoriaService = auditoriaService;
        this.realtimeNotifier = realtimeNotifier;
        this.personagemRepository = personagemRepository;
    }

    /** Nome do personagem que rolou (para exibir no historico), ou null. */
    private String nomePersonagem(Long personagemId) {
        if (personagemId == null) {
            return null;
        }
        return personagemRepository.findById(personagemId)
                .map(com.asus.platform.domain.Personagem::getNome).orElse(null);
    }

    /** Lista o historico. Rolagens ocultas nao reveladas vem com o resultado mascarado. */
    public List<RolagemResponse> listar(Long campanhaId) {
        campanhaService.carregar(campanhaId); // valida existencia
        return rolagemRepository.findByCampanhaIdOrderByCriadoEmDescIdDesc(campanhaId).stream()
                .map(r -> RolagemResponse.de(r, false, nomePersonagem(r.getPersonagemId()))).toList();
    }

    /** Para o Escudo do Mestre (Fase 8): historico completo, sem mascarar ocultas. */
    public List<RolagemResponse> listarCompleto(Long campanhaId) {
        campanhaService.carregar(campanhaId); // valida existencia
        return rolagemRepository.findByCampanhaIdOrderByCriadoEmDescIdDesc(campanhaId).stream()
                .map(r -> RolagemResponse.de(r, true, nomePersonagem(r.getPersonagemId()))).toList();
    }

    @Transactional
    public RolagemResponse rolar(Long campanhaId, RolarRequest req) {
        Campanha campanha = campanhaService.carregar(campanhaId);

        // Rolagem privada (oculta) liberada: o mestre rola escondido e os jogadores veem mascarado.
        boolean oculta = req.oculta();

        ExpressaoDado expressao = ExpressaoDado.parse(req.expressao());
        ResultadoDado resultado = expressao.rolar(dado);

        Integer naturalD20 = (expressao.quantidade() == 1 && expressao.faces() == 20)
                ? resultado.dados().get(0) : null;
        boolean critico = naturalD20 != null && naturalD20 == AsusV1Engine.D20_CRITICO;
        boolean falhaCritica = naturalD20 != null && naturalD20 == AsusV1Engine.D20_FALHA_CRITICA;

        Rolagem rolagem = rolagemRepository.save(Rolagem.builder()
                .campanhaId(campanhaId)
                .personagemId(req.personagemId())
                .usuarioId(req.usuarioId())
                .expressao(expressao.canonico())
                .rotulo(req.rotulo())
                .detalhe(formatarDetalhe(resultado))
                .total(resultado.total())
                .naturalD20(naturalD20)
                .critico(critico)
                .falhaCritica(falhaCritica)
                .oculta(oculta)
                .revelada(false)
                .build());

        auditoriaService.registrar(campanha.getOrganizacaoId(), req.usuarioId(), "ROLAGEM_REALIZADA",
                "Campanha", campanhaId, req.rotulo(), null, String.valueOf(resultado.total()));

        // Tempo real: transmite a versao mascarada (ocultas continuam ocultas para os demais).
        String nome = nomePersonagem(rolagem.getPersonagemId());
        RolagemResponse mascarada = RolagemResponse.de(rolagem, false, nome);
        realtimeNotifier.rolagem(campanhaId, mascarada);
        // Overlay OBS por personagem: publica no topico do personagem que rolou.
        if (rolagem.getPersonagemId() != null) {
            realtimeNotifier.rolagemPersonagem(rolagem.getPersonagemId(), mascarada);
        }

        // Quem rola sempre ve o proprio resultado, mesmo oculto.
        return RolagemResponse.de(rolagem, true, nome);
    }

    @Transactional
    public RolagemResponse revelar(Long campanhaId, Long rolagemId, Long usuarioId) {
        Rolagem rolagem = rolagemRepository.findById(rolagemId)
                .orElseThrow(() -> new NotFoundException("Rolagem " + rolagemId + " nao encontrada"));
        if (!rolagem.getCampanhaId().equals(campanhaId)) {
            throw new IllegalArgumentException("Rolagem nao pertence a campanha " + campanhaId);
        }

        // Quando o usuario e informado, exige permissao de ver rolagens ocultas (plano, secao 9).
        if (usuarioId != null) {
            campanhaService.exigirPermissao(campanhaId, usuarioId, Permissao.VER_ROLAGENS_OCULTAS);
        }

        rolagem.setRevelada(true);
        rolagem = rolagemRepository.save(rolagem);

        Campanha campanha = campanhaService.carregar(campanhaId);
        auditoriaService.registrar(campanha.getOrganizacaoId(), usuarioId, "ROLAGEM_REVELADA",
                "Rolagem", rolagemId, null, null, String.valueOf(rolagem.getTotal()));

        // Tempo real: agora revelada, transmite o conteudo completo.
        RolagemResponse completa = RolagemResponse.de(rolagem, true, nomePersonagem(rolagem.getPersonagemId()));
        realtimeNotifier.rolagem(campanhaId, completa);
        if (rolagem.getPersonagemId() != null) {
            realtimeNotifier.rolagemPersonagem(rolagem.getPersonagemId(), completa);
        }

        return completa;
    }

    private String formatarDetalhe(ResultadoDado r) {
        StringBuilder sb = new StringBuilder(r.dados().toString());
        if (r.modificador() > 0) {
            sb.append("+").append(r.modificador());
        } else if (r.modificador() < 0) {
            sb.append(r.modificador());
        }
        return sb.toString();
    }
}
