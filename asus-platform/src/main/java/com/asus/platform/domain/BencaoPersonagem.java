package com.asus.platform.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;

/** Bencao concedida por uma divindade, salva na ficha (aba Bênçãos). */
@Entity
@Table(name = "bencao_personagem")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class BencaoPersonagem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long personagemId;

    @Column(nullable = false)
    private String nome;

    /** Divindade que concede (livre; a da criacao do personagem e so sugestao). */
    private String divindade;

    /** Custo de ativacao (0 = passiva/sem custo) e o tipo (PE/PM). */
    private Integer custo;
    private String custoTipo;

    @Column(columnDefinition = "TEXT")
    private String efeito;
}
