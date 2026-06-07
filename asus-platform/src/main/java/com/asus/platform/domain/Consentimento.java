package com.asus.platform.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.*;

/** Registro de consentimento LGPD (plano, Seção 19.2). */
@Entity
@Table(name = "consentimento")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Consentimento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long usuarioId;

    /** Tipo de consentimento, ex: TERMOS, PRIVACIDADE, ANALYTICS. */
    @Column(nullable = false)
    private String tipo;

    private String versaoDocumento;

    private boolean aceito;

    @Column(nullable = false)
    private LocalDateTime criadoEm;

    @PrePersist
    void prePersist() {
        if (criadoEm == null) {
            criadoEm = LocalDateTime.now();
        }
    }
}
