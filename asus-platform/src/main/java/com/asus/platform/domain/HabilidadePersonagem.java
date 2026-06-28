package com.asus.platform.domain;

import jakarta.persistence.*;
import lombok.*;

/** Habilidade escolhida por um personagem (aba Habilidades). */
@Entity
@Table(name = "habilidade_personagem")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HabilidadePersonagem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long personagemId;

    @Column(nullable = false)
    private String habilidadeCodigo;

    // Overrides por personagem (null = usa o valor do catalogo).
    private String nomeCustom;
    private String tipoCustom;
    private Integer custoCustom;
    private String custoTipoCustom;

    @Column(columnDefinition = "TEXT")
    private String efeitoCustom;
}
