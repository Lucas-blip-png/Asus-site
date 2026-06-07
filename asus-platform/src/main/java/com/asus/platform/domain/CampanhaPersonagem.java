package com.asus.platform.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.*;

/** Vinculo personagem <-> campanha (rota POST /api/campanhas/{id}/personagens). */
@Entity
@Table(name = "campanha_personagem",
        uniqueConstraints = @UniqueConstraint(columnNames = {"campanhaId", "personagemId"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CampanhaPersonagem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long campanhaId;

    @Column(nullable = false)
    private Long personagemId;

    @Column(nullable = false)
    private LocalDateTime adicionadoEm;

    @PrePersist
    void prePersist() {
        if (adicionadoEm == null) {
            adicionadoEm = LocalDateTime.now();
        }
    }
}
