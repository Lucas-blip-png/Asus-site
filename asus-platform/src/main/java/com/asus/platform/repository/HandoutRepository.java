package com.asus.platform.repository;

import com.asus.platform.domain.Handout;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HandoutRepository extends JpaRepository<Handout, Long> {
    List<Handout> findByCampanhaIdOrderByCriadoEmDescIdDesc(Long campanhaId);
}
