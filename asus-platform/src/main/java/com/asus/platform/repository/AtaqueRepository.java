package com.asus.platform.repository;

import com.asus.platform.domain.Ataque;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AtaqueRepository extends JpaRepository<Ataque, Long> {
    List<Ataque> findByPersonagemId(Long personagemId);
}
