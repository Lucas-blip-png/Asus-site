package com.asus.platform.repository;

import com.asus.platform.domain.HabilidadePersonagem;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HabilidadePersonagemRepository extends JpaRepository<HabilidadePersonagem, Long> {
    List<HabilidadePersonagem> findByPersonagemId(Long personagemId);
    boolean existsByPersonagemIdAndHabilidadeCodigo(Long personagemId, String habilidadeCodigo);
}
