package com.asus.platform.domain;

import jakarta.persistence.*;
import lombok.*;

/** Sistema de jogo. O ASUS e o sistema oficial e padrao (plano, secao 6.3). */
@Entity
@Table(name = "game_system")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GameSystem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Codigo do sistema, ex: ASUS. */
    @Column(nullable = false)
    private String codigo;

    /** Nome de exibicao, ex: ASUS RPG. */
    @Column(nullable = false)
    private String nome;

    /** Versao das regras, ex: ASUS_V1. */
    @Column(nullable = false)
    private String versao;

    private boolean oficial;

    private boolean ativo;
}
