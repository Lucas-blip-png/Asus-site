package com.asus.platform.repository;

import com.asus.platform.domain.Organizacao;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrganizacaoRepository extends JpaRepository<Organizacao, Long> {
    Optional<Organizacao> findBySlug(String slug);
    boolean existsBySlug(String slug);
}
