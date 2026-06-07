package com.asus.platform.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;

/** Feitico construido/conhecido por um personagem (aba Magias). */
@Entity
@Table(name = "feitico_personagem")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class FeiticoPersonagem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long personagemId;

    @Column(nullable = false)
    private String nome;

    private Integer circulo;
    private Integer custoPm;
    private String alcance;
    private String duracao;

    @Column(columnDefinition = "TEXT")
    private String efeito;
}
