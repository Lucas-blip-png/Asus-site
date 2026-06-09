package com.asus.platform.repository;

import com.asus.platform.domain.ItemJogo;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ItemJogoRepository extends JpaRepository<ItemJogo, Long> {
    List<ItemJogo> findByGameSystemId(Long gameSystemId);
    List<ItemJogo> findByGameSystemIdAndCategoria(Long gameSystemId, String categoria);
    Optional<ItemJogo> findByGameSystemIdAndCodigo(Long gameSystemId, String codigo);
}
