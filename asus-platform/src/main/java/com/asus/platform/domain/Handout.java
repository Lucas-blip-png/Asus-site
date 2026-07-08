package com.asus.platform.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.*;

/** Handout: imagem/nota que o mestre entrega aos jogadores durante a sessão. */
@Entity
@Table(name = "handout")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class Handout {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long campanhaId;

    @Column(nullable = false)
    private String titulo;

    /** Imagem (asset id), opcional. */
    private Long assetId;

    @Column(columnDefinition = "TEXT")
    private String texto;

    @Column(nullable = false)
    private LocalDateTime criadoEm;

    @PrePersist
    void prePersist() {
        if (criadoEm == null) {
            criadoEm = LocalDateTime.now();
        }
    }
}
