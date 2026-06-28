package com.asus.platform.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.*;

/** Encontro de combate (rastreador de iniciativa) de uma campanha. */
@Entity
@Table(name = "combate")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class Combate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long campanhaId;

    @Column(nullable = false)
    private String nome;

    /** Rodada atual (começa em 1). */
    private int rodada;

    /** Índice (0-based) do participante da vez, na ordem por iniciativa. */
    private int turnoAtual;

    private boolean ativo;

    private LocalDateTime criadoEm;
}
