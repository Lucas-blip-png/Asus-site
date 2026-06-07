package com.asus.platform.repository;

import com.asus.platform.domain.AnalyticsEvent;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AnalyticsEventRepository extends JpaRepository<AnalyticsEvent, Long> {
    List<AnalyticsEvent> findByOrganizacaoIdOrderByCriadoEmDesc(Long organizacaoId);
}
