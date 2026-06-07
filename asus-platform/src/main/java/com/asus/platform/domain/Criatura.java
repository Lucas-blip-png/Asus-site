package com.asus.platform.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;

/** Criatura do Bestiario ASUS. */
@Entity
@Table(name = "criatura")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class Criatura {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long gameSystemId;

    @Column(nullable = false)
    private String nome;

    private int nivel;
    private String especie;
    private String tipo;

    private int pv;
    private int pm;
    private int pe;
    private int defesa;

    @Column(columnDefinition = "TEXT")
    private String descricao;

    private boolean oficial;
}
