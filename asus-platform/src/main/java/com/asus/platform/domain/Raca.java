package com.asus.platform.domain;

import jakarta.persistence.*;
import lombok.*;

/**
 * Raca de um sistema (plano, secao 6.4).
 *
 * <p>jsonHabilidades pode conter bonus de atributos no formato:
 * {@code {"bonusAtributos": {"vigor": 1, "forca": 1}}}.</p>
 */
@Entity
@Table(name = "raca")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Raca {

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

    private int pvBase;
    private int pmBase;
    private int peBase;

    @Column(columnDefinition = "TEXT")
    private String jsonHabilidades;

    private boolean oficial;
}
