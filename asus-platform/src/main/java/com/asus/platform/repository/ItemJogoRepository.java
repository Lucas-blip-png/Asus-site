package com.asus.platform.repository;

import com.asus.platform.domain.ItemJogo;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ItemJogoRepository extends JpaRepository<ItemJogo, Long> {
    List<ItemJogo> findByGameSystemId(Long gameSystemId);
    List<ItemJogo> findByGameSystemIdAndCategoria(Long gameSystemId, String categoria);
}
