package com.asus.platform.repository;

import com.asus.platform.domain.ItemPersonagem;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ItemPersonagemRepository extends JpaRepository<ItemPersonagem, Long> {
    List<ItemPersonagem> findByPersonagemId(Long personagemId);
}
