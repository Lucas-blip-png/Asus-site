package com.asus.platform.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.*;

/** Assinatura de uma organizacao (plano, Seção 4.3). */
@Entity
@Table(name = "assinatura")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Assinatura {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long organizacaoId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Plano plano;

    /** ATIVA, CANCELADA, INADIMPLENTE... (texto livre nesta fase). */
    @Column(nullable = false)
    private String status;

    @Column(nullable = false)
    private LocalDateTime inicio;

    private LocalDateTime fim;

    /** Ids do gateway de pagamento (vazios enquanto o plano e manual — Seção 4.4). */
    private String gatewayCustomerId;
    private String gatewaySubscriptionId;

    @PrePersist
    void prePersist() {
        if (inicio == null) {
            inicio = LocalDateTime.now();
        }
        if (status == null) {
            status = "ATIVA";
        }
    }
}
