package com.asus.platform.repository;

import com.asus.platform.domain.Pericia;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PericiaRepository extends JpaRepository<Pericia, Long> {
    List<Pericia> findByGameSystemId(Long gameSystemId);
    Optional<Pericia> findByGameSystemIdAndCodigo(Long gameSystemId, String codigo);
}
