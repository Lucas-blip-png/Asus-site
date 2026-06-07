package com.asus.platform.repository;

import com.asus.platform.domain.Assinatura;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AssinaturaRepository extends JpaRepository<Assinatura, Long> {
    Optional<Assinatura> findByOrganizacaoId(Long organizacaoId);
}
