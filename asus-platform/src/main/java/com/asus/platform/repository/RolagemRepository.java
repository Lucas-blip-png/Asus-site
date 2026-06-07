package com.asus.platform.repository;

import com.asus.platform.domain.Rolagem;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RolagemRepository extends JpaRepository<Rolagem, Long> {
    // id como desempate garante ordem estavel mesmo com criadoEm igual.
    List<Rolagem> findByCampanhaIdOrderByCriadoEmDescIdDesc(Long campanhaId);
}
