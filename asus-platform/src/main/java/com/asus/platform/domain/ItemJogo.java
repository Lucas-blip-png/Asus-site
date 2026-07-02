package com.asus.platform.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import lombok.*;

/** Item do catalogo ASUS (Itens e Comercio). Moeda padrao: T$. */
@Entity
@Table(name = "item_jogo")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ItemJogo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long gameSystemId;

    @Column(nullable = false)
    private String codigo;

    @Column(nullable = false)
    private String nome;

    /** ARMA_SIMPLES, ARMA_MARCIAL, ARMADURA, ESCUDO, ITEM_GERAL, FERRAMENTA, ALQUIMICO, VENENO,
     *  ALIMENTACAO, ANIMAL, VEICULO, SERVICO... */
    private String categoria;

    /** Subgrupo da tabela (ex.: "Corpo a Corpo - Leves", "Uma Mao", "Leves", "Venenos"). */
    private String grupo;

    @Column(precision = 12, scale = 2)
    private BigDecimal preco;

    private String moeda;

    // Armas
    private String dano;
    private String critico;
    private String alcance;
    private String tipoDano; // corte/perfuracao/impacto

    /** Espacos de carga (aceita meio-espaco, ex.: 0.5). */
    private Double espacos;

    // Armaduras/escudos
    private Integer bonusDefesa;
    private Integer penalidade;

    @Column(columnDefinition = "TEXT")
    private String efeito;

    private boolean oficial;
}
