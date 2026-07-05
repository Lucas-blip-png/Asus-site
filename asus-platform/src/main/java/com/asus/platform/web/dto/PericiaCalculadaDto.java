package com.asus.platform.web.dto;

public record PericiaCalculadaDto(
        String codigo,
        String nome,
        String atributoBase,
        String sigla,
        int treino,
        int cap,
        int outros,
        int bonus,
        boolean custom) {}
