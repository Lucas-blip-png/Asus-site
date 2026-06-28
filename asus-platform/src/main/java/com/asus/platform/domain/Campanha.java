package com.asus.platform.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.*;

/** Campanha de uma organizacao (plano, secao 8.1). */
@Entity
@Table(name = "campanha")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Campanha {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long organizacaoId;

    /** Usuario que mestra a campanha. */
    private Long mestreId;

    @Column(nullable = false)
    private Long gameSystemId;

    @Column(nullable = false)
    private String nome;

    @Column(columnDefinition = "TEXT")
    private String descricao;

    /** Anotações do mestre sobre a campanha. */
    @Column(columnDefinition = "TEXT")
    private String anotacoes;

    /** Asset de capa (id). Os assets em si chegam na Fase 11. */
    private String capaAssetId;

    @Embedded
    private CampanhaConfig config;

    private boolean arquivada;

    @Column(nullable = false)
    private LocalDateTime criadoEm;

    private LocalDateTime atualizadoEm;

    @PrePersist
    void prePersist() {
        LocalDateTime agora = LocalDateTime.now();
        if (criadoEm == null) {
            criadoEm = agora;
        }
        atualizadoEm = agora;
        if (config == null) {
            // Por padrao o mestre pode fazer rolagens ocultas (Fase 5).
            config = CampanhaConfig.builder().rolagemOcultaPermitida(true).build();
        }
    }

    @PreUpdate
    void preUpdate() {
        atualizadoEm = LocalDateTime.now();
    }
}
