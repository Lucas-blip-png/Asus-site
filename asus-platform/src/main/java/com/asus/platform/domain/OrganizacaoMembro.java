package com.asus.platform.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.*;

/** Vinculo usuario <-> organizacao com papel (plano, secao 3.4). */
@Entity
@Table(name = "organizacao_membro")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrganizacaoMembro {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long organizacaoId;

    @Column(nullable = false)
    private Long usuarioId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PapelOrganizacao papel;

    @Column(nullable = false)
    private LocalDateTime entrouEm;

    @PrePersist
    void prePersist() {
        if (entrouEm == null) {
            entrouEm = LocalDateTime.now();
        }
    }
}
