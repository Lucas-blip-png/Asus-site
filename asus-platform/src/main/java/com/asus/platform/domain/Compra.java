package com.asus.platform.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.*;

/** Compra de um item do marketplace (plano, Seção 14.4). */
@Entity
@Table(name = "compra",
        uniqueConstraints = @UniqueConstraint(columnNames = {"usuarioId", "marketplaceItemId"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Compra {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long usuarioId;

    @Column(nullable = false)
    private Long marketplaceItemId;

    @Column(precision = 12, scale = 2)
    private BigDecimal valorPago;

    private String moeda;

    @Column(nullable = false)
    private LocalDateTime compradoEm;

    @PrePersist
    void prePersist() {
        if (compradoEm == null) {
            compradoEm = LocalDateTime.now();
        }
    }
}
