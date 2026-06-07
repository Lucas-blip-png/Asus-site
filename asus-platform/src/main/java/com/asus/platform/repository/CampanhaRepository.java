package com.asus.platform.repository;

import com.asus.platform.domain.Campanha;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CampanhaRepository extends JpaRepository<Campanha, Long> {
    List<Campanha> findByOrganizacaoIdAndArquivadaFalse(Long organizacaoId);
    long countByOrganizacaoIdAndArquivadaFalse(Long organizacaoId);
}
