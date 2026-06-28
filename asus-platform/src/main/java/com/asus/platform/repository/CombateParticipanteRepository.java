package com.asus.platform.repository;

import com.asus.platform.domain.CombateParticipante;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CombateParticipanteRepository extends JpaRepository<CombateParticipante, Long> {
    List<CombateParticipante> findByCombateIdOrderByIniciativaDescIdAsc(Long combateId);
}
