package com.asus.platform.repository;

import com.asus.platform.domain.MarketplaceItem;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MarketplaceItemRepository extends JpaRepository<MarketplaceItem, Long> {
    List<MarketplaceItem> findByPublicadoTrueOrderByCriadoEmDesc();
    List<MarketplaceItem> findByOficialTrue();
}
