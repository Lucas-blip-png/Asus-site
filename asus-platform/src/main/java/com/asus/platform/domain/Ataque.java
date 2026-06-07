package com.asus.platform.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;

/** Ataque salvo na ficha de um personagem (aba Combate). */
@Entity
@Table(name = "ataque")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class Ataque {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long personagemId;

    @Column(nullable = false)
    private String nome;

    private String dano;
    private String critico;
    private String alcance;
    private String pericia;

    @Column(columnDefinition = "TEXT")
    private String efeito;
}
