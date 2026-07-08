package com.asus.platform.repository;

import com.asus.platform.domain.MensagemCampanha;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MensagemCampanhaRepository extends JpaRepository<MensagemCampanha, Long> {
    List<MensagemCampanha> findTop100ByCampanhaIdOrderByCriadoEmDescIdDesc(Long campanhaId);
    List<MensagemCampanha> findByCampanhaId(Long campanhaId);
}
