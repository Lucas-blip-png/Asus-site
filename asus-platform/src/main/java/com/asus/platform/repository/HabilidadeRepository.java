package com.asus.platform.repository;

import com.asus.platform.domain.Habilidade;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HabilidadeRepository extends JpaRepository<Habilidade, Long> {
    List<Habilidade> findByGameSystemId(Long gameSystemId);
    List<Habilidade> findByGameSystemIdAndClasseCodigo(Long gameSystemId, String classeCodigo);
    List<Habilidade> findByGameSystemIdAndOficialTrue(Long gameSystemId);
}
