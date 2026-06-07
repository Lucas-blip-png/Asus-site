package com.asus.platform.repository;

import com.asus.platform.domain.PresencaSessao;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PresencaSessaoRepository extends JpaRepository<PresencaSessao, Long> {
    List<PresencaSessao> findBySessaoId(Long sessaoId);
    Optional<PresencaSessao> findBySessaoIdAndUsuarioId(Long sessaoId, Long usuarioId);
}
