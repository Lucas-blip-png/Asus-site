package com.asus.platform.repository;

import com.asus.platform.domain.Compra;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CompraRepository extends JpaRepository<Compra, Long> {
    List<Compra> findByUsuarioIdOrderByCompradoEmDesc(Long usuarioId);
    Optional<Compra> findByUsuarioIdAndMarketplaceItemId(Long usuarioId, Long marketplaceItemId);
}
