package com.asus.platform.repository;

import com.asus.platform.domain.Criatura;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CriaturaRepository extends JpaRepository<Criatura, Long> {
    List<Criatura> findByGameSystemId(Long gameSystemId);
    List<Criatura> findByGameSystemIdAndOficialTrue(Long gameSystemId);
}
