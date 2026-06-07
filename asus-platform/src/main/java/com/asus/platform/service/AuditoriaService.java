package com.asus.platform.service;

import com.asus.platform.domain.Auditoria;
import com.asus.platform.repository.AuditoriaRepository;
import java.util.List;
import org.springframework.stereotype.Service;

/** Registro de auditoria basica (plano, secao 10 / criterio de aceite 7). */
@Service
public class AuditoriaService {

    private final AuditoriaRepository repository;

    public AuditoriaService(AuditoriaRepository repository) {
        this.repository = repository;
    }

    public void registrar(Long organizacaoId, Long usuarioId, String acao,
                          String entidade, Long entidadeId,
                          String campo, String valorAnterior, String valorNovo) {
        Auditoria a = Auditoria.builder()
                .organizacaoId(organizacaoId)
                .usuarioId(usuarioId)
                .acao(acao)
                .entidade(entidade)
                .entidadeId(entidadeId)
                .campo(campo)
                .valorAnterior(valorAnterior)
                .valorNovo(valorNovo)
                .build();
        repository.save(a);
    }

    public List<Auditoria> historico(String entidade, Long entidadeId) {
        return repository.findByEntidadeAndEntidadeIdOrderByCriadoEmDesc(entidade, entidadeId);
    }
}
