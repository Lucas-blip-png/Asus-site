package com.asus.platform.domain;

import jakarta.persistence.*;
import lombok.*;

/** Classe de um sistema (plano, secao 6.5). */
@Entity
@Table(name = "classe")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Classe {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long gameSystemId;

    @Column(nullable = false)
    private String codigo;

    @Column(nullable = false)
    private String nome;

    @Column(columnDefinition = "TEXT")
    private String descricao;

    private int multiplicadorPv;
    private int multiplicadorPm;
    private int multiplicadorPe;

    @Column(columnDefinition = "TEXT")
    private String jsonTrilhas;

    @Column(columnDefinition = "TEXT")
    private String jsonPassiva;

    /** Bonus inicial: {"atributos":{"forca":2,...},"pericias":{"vigor":2,...},"slots":1}. */
    @Column(columnDefinition = "TEXT")
    private String jsonBonus;

    /** Se preenchido, esta "classe" e uma trilha da classe-pai (codigo). */
    private String classePaiCodigo;

    private boolean oficial;
}
