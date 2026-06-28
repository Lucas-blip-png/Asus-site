package com.asus.platform.repository;

import com.asus.platform.domain.Combate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CombateRepository extends JpaRepository<Combate, Long> {
    List<Combate> findByCampanhaIdOrderByCriadoEmDesc(Long campanhaId);
}
