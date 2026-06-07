package com.asus.platform.repository;

import com.asus.platform.domain.Classe;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClasseRepository extends JpaRepository<Classe, Long> {
    List<Classe> findByGameSystemId(Long gameSystemId);
    Optional<Classe> findByGameSystemIdAndCodigo(Long gameSystemId, String codigo);
}
