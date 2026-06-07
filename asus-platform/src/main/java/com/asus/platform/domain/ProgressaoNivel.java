package com.asus.platform.domain;

import jakarta.persistence.*;
import lombok.*;

/** Linha da tabela de progressao de nivel (planilha "Sistema de Nivel"). */
@Entity
@Table(name = "progressao_nivel")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProgressaoNivel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long gameSystemId;

    private int nivel;
    private int xpNecessario;

    /** Foco de progressao do nivel (Classe Primaria, Trilha primaria, etc.). */
    private String foco;

    private String recompensa;

    /** Limite maximo por atributo neste nivel. */
    private int limiteAtributo;
}
