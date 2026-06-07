package com.asus.platform.repository;

import com.asus.platform.domain.Asset;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface AssetRepository extends JpaRepository<Asset, Long> {
    List<Asset> findByOrganizacaoId(Long organizacaoId);

    @Query("select coalesce(sum(a.tamanhoBytes), 0) from Asset a where a.organizacaoId = ?1")
    long somaBytesPorOrganizacao(Long organizacaoId);
}
