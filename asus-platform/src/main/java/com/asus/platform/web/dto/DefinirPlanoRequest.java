package com.asus.platform.web.dto;

import com.asus.platform.domain.Plano;
import jakarta.validation.constraints.NotNull;

public record DefinirPlanoRequest(@NotNull Plano plano) {}
