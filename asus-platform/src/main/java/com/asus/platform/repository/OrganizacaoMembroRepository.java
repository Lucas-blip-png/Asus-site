package com.asus.platform.repository;

import com.asus.platform.domain.OrganizacaoMembro;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrganizacaoMembroRepository extends JpaRepository<OrganizacaoMembro, Long> {
    List<OrganizacaoMembro> findByOrganizacaoId(Long organizacaoId);
    List<OrganizacaoMembro> findByUsuarioId(Long usuarioId);
}
