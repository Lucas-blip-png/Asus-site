package com.asus.platform.repository;

import com.asus.platform.domain.PersonagemSnapshot;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PersonagemSnapshotRepository extends JpaRepository<PersonagemSnapshot, Long> {
    List<PersonagemSnapshot> findByPersonagemIdOrderByCriadoEmDesc(Long personagemId);
}
