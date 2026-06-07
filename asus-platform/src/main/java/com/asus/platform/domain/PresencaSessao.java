package com.asus.platform.domain;

import jakarta.persistence.*;
import lombok.*;

/** Presenca de um usuario em uma sessao (plano, Seção 18.2). */
@Entity
@Table(name = "presenca_sessao",
        uniqueConstraints = @UniqueConstraint(columnNames = {"sessaoId", "usuarioId"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PresencaSessao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long sessaoId;

    @Column(nullable = false)
    private Long usuarioId;

    /** CONFIRMADO, RECUSADO, TALVEZ. */
    private String status;
}
