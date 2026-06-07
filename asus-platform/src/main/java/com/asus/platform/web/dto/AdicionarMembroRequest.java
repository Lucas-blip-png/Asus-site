package com.asus.platform.web.dto;

import com.asus.platform.domain.PapelOrganizacao;
import jakarta.validation.constraints.NotNull;

public record AdicionarMembroRequest(@NotNull Long usuarioId, PapelOrganizacao papel) {}
