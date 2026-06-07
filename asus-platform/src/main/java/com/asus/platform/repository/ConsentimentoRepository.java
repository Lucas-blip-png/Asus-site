package com.asus.platform.repository;

import com.asus.platform.domain.Consentimento;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConsentimentoRepository extends JpaRepository<Consentimento, Long> {
    List<Consentimento> findByUsuarioIdOrderByCriadoEmDesc(Long usuarioId);
}
