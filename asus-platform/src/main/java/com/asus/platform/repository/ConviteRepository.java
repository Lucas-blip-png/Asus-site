package com.asus.platform.repository;

import com.asus.platform.domain.Convite;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConviteRepository extends JpaRepository<Convite, Long> {
    Optional<Convite> findByCodigo(String codigo);
    boolean existsByCodigo(String codigo);
    List<Convite> findByCampanhaId(Long campanhaId);
}
