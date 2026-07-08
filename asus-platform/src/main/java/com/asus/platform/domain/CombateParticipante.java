package com.asus.platform.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;

/** Participante de um combate (agente ou ameaça), com iniciativa e PV. */
@Entity
@Table(name = "combate_participante")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class CombateParticipante {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long combateId;

    /** Personagem vinculado (null para ameaças/NPCs). */
    private Long personagemId;

    private Long avatarAssetId;

    @Column(nullable = false)
    private String nome;

    private int iniciativa;
    private int pvAtual;
    private int pvMax;

    /** true = ameaça/inimigo; false = agente. */
    private boolean inimigo;

    /** Condições/efeitos como JSON: [{"nome":"Envenenado","turnos":3}, ...]. */
    @Column(columnDefinition = "TEXT")
    private String condicoes;

    /** Posição no mapa tático (célula do grid; null = fora do mapa). */
    private Integer posX;
    private Integer posY;
}
