package com.asus.platform.service.storage;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

/**
 * Armazenamento de assets em S3 / Cloudflare R2 / GCS (S3-compativel).
 *
 * <p>Ativo quando {@code asus.storage.tipo=s3}. Para R2/GCS defina
 * {@code asus.storage.s3.endpoint} (path-style). Ver {@code INTEGRACOES.md}.</p>
 */
@Component
@ConditionalOnProperty(name = "asus.storage.tipo", havingValue = "s3")
public class ArmazenamentoS3 implements ArmazenamentoAssets {

    private final S3Client s3;
    private final String bucket;

    public ArmazenamentoS3(
            @Value("${asus.storage.s3.bucket}") String bucket,
            @Value("${asus.storage.s3.region:auto}") String region,
            @Value("${asus.storage.s3.endpoint:}") String endpoint,
            @Value("${asus.storage.s3.access-key}") String accessKey,
            @Value("${asus.storage.s3.secret-key}") String secretKey) {
        this.bucket = bucket;
        var builder = S3Client.builder()
                .region(Region.of(region))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(accessKey, secretKey)));
        if (endpoint != null && !endpoint.isBlank()) {
            builder.endpointOverride(URI.create(endpoint)).forcePathStyle(true); // R2/GCS/MinIO
        }
        this.s3 = builder.build();
    }

    @Override
    public String gravar(Long organizacaoId, String nomeOriginal, InputStream conteudo, long tamanho)
            throws IOException {
        String key = organizacaoId + "/" + UUID.randomUUID() + "_" + nomeOriginal;
        s3.putObject(PutObjectRequest.builder().bucket(bucket).key(key).build(),
                RequestBody.fromInputStream(conteudo, tamanho));
        return key;
    }

    @Override
    public byte[] ler(String storagePath) throws IOException {
        ResponseBytes<GetObjectResponse> obj = s3.getObjectAsBytes(
                GetObjectRequest.builder().bucket(bucket).key(storagePath).build());
        return obj.asByteArray();
    }

    @Override
    public void apagar(String storagePath) {
        s3.deleteObject(DeleteObjectRequest.builder().bucket(bucket).key(storagePath).build());
    }
}
