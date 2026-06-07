package com.asus.platform.web.dto;

import jakarta.validation.constraints.NotNull;

public record AdicionarPersonagemCampanhaRequest(@NotNull Long personagemId) {}
