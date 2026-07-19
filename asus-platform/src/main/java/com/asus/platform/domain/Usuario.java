package com.asus.platform.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.*;

/** Usuario da plataforma. Na Fase 1 existe um usuario "dev" semeado no boot. */
@Entity
@Table(name = "usuario")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nome;

    @Column(nullable = false, unique = true)
    private String email;

    /** Hash BCrypt da senha (Fase 7). Null para contas so-OAuth. */
    private String senhaHash;

    @Column(nullable = false)
    private LocalDateTime criadoEm;

    /** Marca conta cujos dados pessoais foram anonimizados (LGPD, Fase 13). */
    private boolean anonimizado;

    /** E-mail confirmado via link de verificacao. Contas OAuth ja entram verificadas. */
    private boolean emailVerificado;

    @PrePersist
    void prePersist() {
        if (criadoEm == null) {
            criadoEm = LocalDateTime.now();
        }
    }
}
