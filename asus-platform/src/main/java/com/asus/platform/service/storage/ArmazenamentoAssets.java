package com.asus.platform.service.storage;

import java.io.IOException;
import java.io.InputStream;

/**
 * Abstração de armazenamento de assets (Fase 11).
 *
 * <p>A implementação padrão é {@link ArmazenamentoLocal} (disco local). Para nuvem
 * (S3/R2/GCS) basta fornecer outra implementação ativada por
 * {@code asus.storage.tipo=s3} — veja {@code INTEGRACOES.md} na raiz do repo.</p>
 */
public interface ArmazenamentoAssets {

    /**
     * Grava o conteúdo e devolve o {@code storagePath} (identificador/caminho) que
     * deve ser persistido no {@code Asset} para posterior leitura/remoção.
     */
    String gravar(Long organizacaoId, String nomeOriginal, InputStream conteudo, long tamanho) throws IOException;

    /** Lê os bytes a partir do {@code storagePath} previamente gravado. */
    byte[] ler(String storagePath) throws IOException;

    /** Remove o arquivo (idempotente: não falha se já não existir). */
    void apagar(String storagePath);
}
