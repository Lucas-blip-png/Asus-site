package com.asus.platform.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import java.math.BigDecimal;
import lombok.*;

/** Item no inventario de um personagem (aba Inventário). Pode vir do catalogo ou ser proprio. */
@Entity
@Table(name = "item_personagem")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class ItemPersonagem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long personagemId;

    @Column(nullable = false)
    private String nome;

    private String categoria;

    /** Espaços ocupados por unidade (peso/carga ASUS = Força x 2). */
    private Integer espacos;
    private Integer quantidade;
    private boolean equipado;

    // Armas
    private String dano;
    private String critico;
    private String alcance;
    private String tipoDano;

    // Armaduras/escudos
    private Integer bonusDefesa;
    private Integer penalidade;

    @Column(precision = 12, scale = 2)
    private BigDecimal preco;
    private String moeda;

    @Column(columnDefinition = "TEXT")
    private String efeito;

    /** Codigo do item de catalogo de origem (null se item proprio). */
    private String itemJogoCodigo;
}
