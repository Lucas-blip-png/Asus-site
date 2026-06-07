package com.asus.platform.repository;

import com.asus.platform.domain.Sessao;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SessaoRepository extends JpaRepository<Sessao, Long> {
    List<Sessao> findByCampanhaIdOrderByInicio(Long campanhaId);
}
