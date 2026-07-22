package com.asus.platform.repository;

import com.asus.platform.domain.Cena;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CenaRepository extends JpaRepository<Cena, Long> {
    List<Cena> findByCampanhaIdOrderByCriadoEmDescIdDesc(Long campanhaId);
}
