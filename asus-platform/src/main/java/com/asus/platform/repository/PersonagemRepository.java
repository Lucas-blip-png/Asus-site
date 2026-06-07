package com.asus.platform.repository;

import com.asus.platform.domain.Personagem;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PersonagemRepository extends JpaRepository<Personagem, Long> {
    List<Personagem> findByOrganizacaoIdAndArquivadoFalse(Long organizacaoId);
    long countByOrganizacaoIdAndArquivadoFalse(Long organizacaoId);
    List<Personagem> findByUsuarioId(Long usuarioId);
}
