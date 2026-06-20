package com.asus.platform.service.storage;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Armazenamento em disco local (padrão). Grava em {@code asus.uploads-dir}
 * (padrão {@code uploads/}); em produção aponte para um volume persistente.
 *
 * <p>Ativo quando {@code asus.storage.tipo=local} (ou ausente).</p>
 */
@Component
@ConditionalOnProperty(name = "asus.storage.tipo", havingValue = "local", matchIfMissing = true)
public class ArmazenamentoLocal implements ArmazenamentoAssets {

    private final Path uploadsRoot;

    public ArmazenamentoLocal(@Value("${asus.uploads-dir:uploads}") String uploadsDir) {
        this.uploadsRoot = Paths.get(uploadsDir).toAbsolutePath().normalize();
    }

    @Override
    public String gravar(Long organizacaoId, String nomeOriginal, InputStream conteudo, long tamanho)
            throws IOException {
        Path dirOrg = uploadsRoot.resolve(String.valueOf(organizacaoId));
        Files.createDirectories(dirOrg);
        Path destino = dirOrg.resolve(UUID.randomUUID() + "_" + nomeOriginal);
        Files.copy(conteudo, destino, StandardCopyOption.REPLACE_EXISTING);
        return uploadsRoot.relativize(destino).toString().replace('\\', '/');
    }

    @Override
    public byte[] ler(String storagePath) throws IOException {
        Path arquivo = uploadsRoot.resolve(storagePath).normalize();
        if (!arquivo.startsWith(uploadsRoot)) {
            throw new IllegalArgumentException("Caminho de asset invalido");
        }
        return Files.readAllBytes(arquivo);
    }

    @Override
    public void apagar(String storagePath) {
        try {
            Files.deleteIfExists(uploadsRoot.resolve(storagePath).normalize());
        } catch (IOException ignored) {
            // arquivo ja removido: segue para apagar o registro
        }
    }
}
