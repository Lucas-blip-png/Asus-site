package com.asus.platform.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.*;

/** Rolagem de dados de uma campanha (plano, secao 21.5). */
@Entity
@Table(name = "rolagem")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Rolagem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long campanhaId;

    private Long personagemId;

    /** Quem rolou. Na Fase 7 (login real) virá do JWT. */
    private Long usuarioId;

    /** Expressao canonica rolada, ex: 1d20+5. */
    @Column(nullable = false)
    private String expressao;

    /** Rotulo livre, ex: "Ataque", "Percepcao". */
    private String rotulo;

    /** Detalhamento das faces sorteadas, ex: "[14]+5". */
    @Column(nullable = false)
    private String detalhe;

    private int total;

    /** Face natural do d20 quando a rolagem e um unico d20 (para critico/falha). */
    private Integer naturalD20;

    private boolean critico;
    private boolean falhaCritica;

    /** Rolagem feita as ocultas pelo mestre (plano, secao 8.2 / criterio Fase 5). */
    private boolean oculta;
    private boolean revelada;

    @Column(nullable = false)
    private LocalDateTime criadoEm;

    @PrePersist
    void prePersist() {
        if (criadoEm == null) {
            criadoEm = LocalDateTime.now();
        }
    }
}
