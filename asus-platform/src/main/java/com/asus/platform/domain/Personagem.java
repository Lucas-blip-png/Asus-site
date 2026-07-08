package com.asus.platform.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.*;

/** Personagem ASUS (plano, secao 7.1). */
@Entity
@Table(name = "personagem")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Personagem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long organizacaoId;

    private Long usuarioId;

    @Column(nullable = false)
    private Long gameSystemId;

    /** Versao das regras com que a ficha foi criada (ex: ASUS_V1). */
    @Column(nullable = false)
    private String rulesetVersion;

    @Column(nullable = false)
    private String nome;

    private String jogador;

    private Long racaId;
    private Long classeId;
    private Long trilhaId;
    /** Multiclasse: classe e trilha secundárias (opcionais). */
    private Long classeSecundariaId;
    private Long trilhaSecundariaId;

    /** Asset (Fase 11) usado como foto/retrato da ficha. */
    private Long avatarAssetId;

    private int nivel;
    private int xpAtual;

    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "forca", column = @Column(name = "base_forca")),
        @AttributeOverride(name = "constituicao", column = @Column(name = "base_constituicao")),
        @AttributeOverride(name = "destreza", column = @Column(name = "base_destreza")),
        @AttributeOverride(name = "agilidade", column = @Column(name = "base_agilidade")),
        @AttributeOverride(name = "inteligencia", column = @Column(name = "base_inteligencia")),
        @AttributeOverride(name = "sabedoria", column = @Column(name = "base_sabedoria")),
        @AttributeOverride(name = "carisma", column = @Column(name = "base_carisma"))
    })
    private Atributos atributosBase;

    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "forca", column = @Column(name = "final_forca")),
        @AttributeOverride(name = "constituicao", column = @Column(name = "final_constituicao")),
        @AttributeOverride(name = "destreza", column = @Column(name = "final_destreza")),
        @AttributeOverride(name = "agilidade", column = @Column(name = "final_agilidade")),
        @AttributeOverride(name = "inteligencia", column = @Column(name = "final_inteligencia")),
        @AttributeOverride(name = "sabedoria", column = @Column(name = "final_sabedoria")),
        @AttributeOverride(name = "carisma", column = @Column(name = "final_carisma"))
    })
    private Atributos atributosFinais;

    @Embedded
    private Status status;

    // Tetos manuais de PV/PM/PE (override). Null = usa o valor calculado pelas regras.
    private Integer pvMaxManual;
    private Integer pmMaxManual;
    private Integer peMaxManual;

    @Column(columnDefinition = "TEXT")
    private String anotacoes;

    @Column(columnDefinition = "TEXT")
    private String aparencia;

    @Column(columnDefinition = "TEXT")
    private String personalidade;

    @Column(columnDefinition = "TEXT")
    private String historico;

    @Column(columnDefinition = "TEXT")
    private String objetivo;

    /** Divindade escolhida (passo 4 da criacao). */
    private String divindade;

    /** Pontos de treino de pericias: {"VIGOR":2,"COMBATE":3,...}. */
    @Column(columnDefinition = "TEXT")
    private String jsonPericias;

    /** Pericias "Outros" (concedidas por itens): [{"nome":..,"atributo":..,"treino":..}]. */
    @Column(columnDefinition = "TEXT")
    private String jsonPericiasCustom;

    /** Bonus "Outros" por pericia padrao (sem limite): {"VIGOR":2,"COMBATE":1,...}. */
    @Column(columnDefinition = "TEXT")
    private String jsonPericiasOutros;

    /** Dinheiro do personagem em T$ (aceita meio tibar). */
    private Double dinheiro;

    /** Token do link público da ficha (null = não compartilhada). */
    private String shareToken;

    private boolean arquivado;

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
        if (nivel < 1) {
            nivel = 1;
        }
    }

    @PreUpdate
    void preUpdate() {
        atualizadoEm = LocalDateTime.now();
    }
}
