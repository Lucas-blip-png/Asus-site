package com.asus.platform.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.*;

/**
 * Convite por codigo para entrar em uma campanha
 * (rotas POST /api/campanhas/{id}/convites e POST /api/campanhas/entrar/{codigo}).
 */
@Entity
@Table(name = "convite")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Convite {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long campanhaId;

    @Column(nullable = false, unique = true)
    private String codigo;

    /** Papel concedido a quem entrar com este convite. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PapelCampanha papel;

    private Long criadoPorUsuarioId;

    /** Numero maximo de usos. null = ilimitado. */
    private Integer maxUsos;

    private int usos;

    /** Expiracao opcional. null = nunca expira. */
    private LocalDateTime expiraEm;

    private boolean ativo;

    @Column(nullable = false)
    private LocalDateTime criadoEm;

    @PrePersist
    void prePersist() {
        if (criadoEm == null) {
            criadoEm = LocalDateTime.now();
        }
    }

    /** Convite ainda pode ser usado? (ativo, dentro do prazo e dos usos). */
    public boolean utilizavel(LocalDateTime agora) {
        if (!ativo) {
            return false;
        }
        if (expiraEm != null && agora.isAfter(expiraEm)) {
            return false;
        }
        return maxUsos == null || usos < maxUsos;
    }
}
