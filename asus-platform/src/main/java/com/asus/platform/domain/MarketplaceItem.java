package com.asus.platform.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.*;

/** Item a venda/distribuicao no marketplace (plano, Seção 14.3). */
@Entity
@Table(name = "marketplace_item")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MarketplaceItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long autorUsuarioId;

    @Column(nullable = false)
    private String titulo;

    @Column(columnDefinition = "TEXT")
    private String descricao;

    /** Tipo livre: aventura, raca, classe, mapa, token, template... (Seção 14.2). */
    private String tipo;

    @Column(precision = 12, scale = 2)
    private BigDecimal preco;

    private String moeda;

    private boolean gratuito;

    private boolean publicado;

    /** Item de vitrine semeado pelo sistema (para o refresh gerenciar). */
    private boolean oficial;

    @Column(nullable = false)
    private LocalDateTime criadoEm;

    @PrePersist
    void prePersist() {
        if (criadoEm == null) {
            criadoEm = LocalDateTime.now();
        }
        if (moeda == null) {
            moeda = "BRL";
        }
    }
}
