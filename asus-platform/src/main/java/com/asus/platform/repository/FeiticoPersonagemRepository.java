package com.asus.platform.repository;

import com.asus.platform.domain.FeiticoPersonagem;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FeiticoPersonagemRepository extends JpaRepository<FeiticoPersonagem, Long> {
    List<FeiticoPersonagem> findByPersonagemId(Long personagemId);
}
