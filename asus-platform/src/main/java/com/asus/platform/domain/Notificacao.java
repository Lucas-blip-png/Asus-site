package com.asus.platform.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.*;

/** Notificacao para um usuario (plano, Seção 17). */
@Entity
@Table(name = "notificacao")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notificacao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long usuarioId;

    private String titulo;

    @Column(columnDefinition = "TEXT")
    private String mensagem;

    private String tipo;

    private boolean lida;

    @Column(nullable = false)
    private LocalDateTime criadaEm;

    @PrePersist
    void prePersist() {
        if (criadaEm == null) {
            criadaEm = LocalDateTime.now();
        }
    }
}
