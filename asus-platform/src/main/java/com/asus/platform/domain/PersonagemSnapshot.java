package com.asus.platform.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.*;

/** Retrato completo da ficha em um momento (plano, secao 11.2). */
@Entity
@Table(name = "personagem_snapshot")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PersonagemSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long personagemId;

    /** Motivo do snapshot, ex: CRIACAO, LEVEL_UP. */
    @Column(nullable = false)
    private String motivo;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String jsonFicha;

    @Column(nullable = false)
    private LocalDateTime criadoEm;

    @PrePersist
    void prePersist() {
        if (criadoEm == null) {
            criadoEm = LocalDateTime.now();
        }
    }
}
