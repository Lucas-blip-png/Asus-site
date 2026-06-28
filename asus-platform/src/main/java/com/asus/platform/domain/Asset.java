package com.asus.platform.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.*;

/** Arquivo/midia de uma organizacao (plano, Seção 13.1). */
@Entity
@Table(name = "asset")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Asset {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long organizacaoId;

    private Long usuarioId;

    /** Um valor de {@link TipoAsset}. */
    @Column(nullable = false)
    private String tipo;

    @Column(nullable = false)
    private String nomeOriginal;

    /** Caminho relativo dentro do diretorio de uploads. */
    @Column(nullable = false)
    private String storagePath;

    private String mimeType;

    private long tamanhoBytes;

    /** Conteudo em base64 (persiste no banco; sobrevive a redeploys do container efemero). */
    @Column(columnDefinition = "TEXT")
    private String dadosBase64;

    private boolean publico;

    @Column(nullable = false)
    private LocalDateTime criadoEm;

    @PrePersist
    void prePersist() {
        if (criadoEm == null) {
            criadoEm = LocalDateTime.now();
        }
    }
}
