package com.asus.platform.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.*;

/** Vinculo usuario <-> campanha com papel (plano, secao 9.2). */
@Entity
@Table(name = "campanha_membro",
        uniqueConstraints = @UniqueConstraint(columnNames = {"campanhaId", "usuarioId"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CampanhaMembro {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long campanhaId;

    @Column(nullable = false)
    private Long usuarioId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PapelCampanha papel;

    @Column(nullable = false)
    private LocalDateTime entrouEm;

    @PrePersist
    void prePersist() {
        if (entrouEm == null) {
            entrouEm = LocalDateTime.now();
        }
    }
}
