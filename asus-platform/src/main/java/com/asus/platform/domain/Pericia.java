package com.asus.platform.domain;

import jakarta.persistence.*;
import lombok.*;

/** Pericia de um sistema (plano, secao 6.6). atributoBase referencia um Atributo. */
@Entity
@Table(name = "pericia")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Pericia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long gameSystemId;

    @Column(nullable = false)
    private String codigo;

    @Column(nullable = false)
    private String nome;

    /** Nome do enum Atributo (FORCA, AGILIDADE, ...). */
    @Column(nullable = false)
    private String atributoBase;

    private boolean oficial;
}
