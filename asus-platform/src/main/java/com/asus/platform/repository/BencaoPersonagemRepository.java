package com.asus.platform.repository;

import com.asus.platform.domain.BencaoPersonagem;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BencaoPersonagemRepository extends JpaRepository<BencaoPersonagem, Long> {
    List<BencaoPersonagem> findByPersonagemId(Long personagemId);
}
