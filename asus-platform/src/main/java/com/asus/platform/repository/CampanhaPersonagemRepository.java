package com.asus.platform.repository;

import com.asus.platform.domain.CampanhaPersonagem;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CampanhaPersonagemRepository extends JpaRepository<CampanhaPersonagem, Long> {
    List<CampanhaPersonagem> findByCampanhaId(Long campanhaId);
    List<CampanhaPersonagem> findByPersonagemId(Long personagemId);
    boolean existsByCampanhaIdAndPersonagemId(Long campanhaId, Long personagemId);
}
