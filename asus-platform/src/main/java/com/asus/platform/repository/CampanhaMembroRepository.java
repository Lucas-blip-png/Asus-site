package com.asus.platform.repository;

import com.asus.platform.domain.CampanhaMembro;
import com.asus.platform.domain.PapelCampanha;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CampanhaMembroRepository extends JpaRepository<CampanhaMembro, Long> {
    List<CampanhaMembro> findByCampanhaId(Long campanhaId);
    Optional<CampanhaMembro> findByCampanhaIdAndUsuarioId(Long campanhaId, Long usuarioId);
    boolean existsByCampanhaIdAndUsuarioId(Long campanhaId, Long usuarioId);
    long countByCampanhaIdAndPapel(Long campanhaId, PapelCampanha papel);
    List<CampanhaMembro> findByUsuarioId(Long usuarioId);
}
