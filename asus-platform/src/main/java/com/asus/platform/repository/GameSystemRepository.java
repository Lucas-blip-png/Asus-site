package com.asus.platform.repository;

import com.asus.platform.domain.GameSystem;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GameSystemRepository extends JpaRepository<GameSystem, Long> {
    Optional<GameSystem> findByCodigo(String codigo);
    boolean existsByCodigo(String codigo);
}
