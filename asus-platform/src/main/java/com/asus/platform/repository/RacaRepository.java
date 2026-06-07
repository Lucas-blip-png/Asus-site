package com.asus.platform.repository;

import com.asus.platform.domain.Raca;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RacaRepository extends JpaRepository<Raca, Long> {
    List<Raca> findByGameSystemId(Long gameSystemId);
    Optional<Raca> findByGameSystemIdAndCodigo(Long gameSystemId, String codigo);
}
