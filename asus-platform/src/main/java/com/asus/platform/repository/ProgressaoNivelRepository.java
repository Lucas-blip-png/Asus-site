package com.asus.platform.repository;

import com.asus.platform.domain.ProgressaoNivel;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProgressaoNivelRepository extends JpaRepository<ProgressaoNivel, Long> {
    List<ProgressaoNivel> findByGameSystemIdOrderByNivel(Long gameSystemId);
}
