package com.asus.platform.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.*;

/**
 * Cena/seção com mapa 2D: fundo (imagem), névoa de guerra e tokens.
 * O mestre monta; os jogadores enxergam a cena ATIVA da campanha em tempo real.
 */
@Entity
@Table(name = "cena")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class Cena {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long campanhaId;

    @Column(nullable = false)
    private String nome;

    /** Imagem de fundo do mapa (asset id). */
    private Long mapaAssetId;

    /** Névoa de guerra: JSON com as células REVELADAS (ex.: [0,1,15]); null = névoa desligada. */
    @Column(columnDefinition = "TEXT")
    private String fogJson;

    /** Tokens no mapa: JSON [{id,nome,cor,x,y,avatarAssetId}]. */
    @Column(columnDefinition = "TEXT")
    private String tokensJson;

    /** Cena atualmente exibida aos jogadores (só uma ativa por campanha). */
    private boolean ativa;

    @Column(nullable = false)
    private LocalDateTime criadoEm;

    @PrePersist
    void prePersist() {
        if (criadoEm == null) {
            criadoEm = LocalDateTime.now();
        }
    }
}
