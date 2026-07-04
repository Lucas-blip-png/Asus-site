package com.asus.platform.domain;

import jakarta.persistence.*;
import lombok.*;

/** Habilidade de classe ou geral (livro ASUS, "Habilidades de Classe"). */
@Entity
@Table(name = "habilidade")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Habilidade {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long gameSystemId;

    @Column(nullable = false)
    private String codigo;

    @Column(nullable = false)
    private String nome;

    /** Classe/trilha dona, ou "GERAL" para habilidades gerais. */
    private String classeCodigo;

    /** ATIVA ou PASSIVA. */
    private String tipo;

    /** Custo em pontos (0 se passiva/sem custo). */
    private int custo;

    /** PE ou PM. */
    private String custoTipo;

    @Column(columnDefinition = "TEXT")
    private String requisito;

    /** Pre-requisitos derivados para liberar a habilidade (gating). */
    private int nivelMinimo;
    private String atributoRequisito;
    private int valorAtributoRequisito;

    @Column(columnDefinition = "TEXT")
    private String efeito;

    private boolean oficial;
}
