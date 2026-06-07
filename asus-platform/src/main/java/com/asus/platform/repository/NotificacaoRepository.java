package com.asus.platform.repository;

import com.asus.platform.domain.Notificacao;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificacaoRepository extends JpaRepository<Notificacao, Long> {
    List<Notificacao> findByUsuarioIdOrderByCriadaEmDesc(Long usuarioId);
    long countByUsuarioIdAndLidaFalse(Long usuarioId);
}
