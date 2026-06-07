package com.asus.platform.repository;

import com.asus.platform.domain.Template;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TemplateRepository extends JpaRepository<Template, Long> {
    List<Template> findByOrganizacaoId(Long organizacaoId);
    List<Template> findByPublicoTrueOrderByCriadoEmDesc();
}
