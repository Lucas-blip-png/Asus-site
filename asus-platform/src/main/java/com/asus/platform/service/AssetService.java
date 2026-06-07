package com.asus.platform.service;

import com.asus.platform.domain.Asset;
import com.asus.platform.domain.LimitesPlano;
import com.asus.platform.domain.TipoAsset;
import com.asus.platform.repository.AssetRepository;
import com.asus.platform.web.LimiteExcedidoException;
import com.asus.platform.web.NotFoundException;
import com.asus.platform.web.dto.AssetResponse;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

/**
 * Upload/listagem/download/exclusao de assets em storage local (plano, Seção 13 / Fase 11).
 *
 * <p>MVP grava em {@code asus.uploads-dir} (padrao {@code uploads/}). Em producao,
 * trocar por S3/R2/GCS implementando outra estrategia de gravacao.</p>
 */
@Service
public class AssetService {

    private final AssetRepository assetRepository;
    private final OrganizacaoService organizacaoService;
    private final PlanoService planoService;
    private final AuditoriaService auditoriaService;
    private final Path uploadsRoot;

    public AssetService(AssetRepository assetRepository,
                        OrganizacaoService organizacaoService,
                        PlanoService planoService,
                        AuditoriaService auditoriaService,
                        @Value("${asus.uploads-dir:uploads}") String uploadsDir) {
        this.assetRepository = assetRepository;
        this.organizacaoService = organizacaoService;
        this.planoService = planoService;
        this.auditoriaService = auditoriaService;
        this.uploadsRoot = Paths.get(uploadsDir).toAbsolutePath().normalize();
    }

    public List<AssetResponse> listar(Long organizacaoId) {
        organizacaoService.buscar(organizacaoId); // valida existencia
        return assetRepository.findByOrganizacaoId(organizacaoId).stream()
                .map(AssetResponse::de).toList();
    }

    @Transactional
    public AssetResponse upload(Long organizacaoId, String tipo, boolean publico,
                                Long usuarioId, MultipartFile arquivo) {
        organizacaoService.buscar(organizacaoId); // valida existencia
        if (arquivo == null || arquivo.isEmpty()) {
            throw new IllegalArgumentException("Arquivo vazio");
        }

        long tamanho = arquivo.getSize();
        LimitesPlano limites = planoService.limitesDa(organizacaoId);
        long usado = assetRepository.somaBytesPorOrganizacao(organizacaoId);
        if (usado + tamanho > limites.assetsBytesMax()) {
            throw new LimiteExcedidoException("Cota de assets do plano excedida");
        }

        String nomeOriginal = StringUtils.cleanPath(
                arquivo.getOriginalFilename() == null ? "arquivo" : arquivo.getOriginalFilename());
        String storagePath;
        try {
            Path dirOrg = uploadsRoot.resolve(String.valueOf(organizacaoId));
            Files.createDirectories(dirOrg);
            Path destino = dirOrg.resolve(UUID.randomUUID() + "_" + nomeOriginal);
            try (InputStream in = arquivo.getInputStream()) {
                Files.copy(in, destino, StandardCopyOption.REPLACE_EXISTING);
            }
            storagePath = uploadsRoot.relativize(destino).toString().replace('\\', '/');
        } catch (IOException ex) {
            throw new IllegalStateException("Falha ao gravar o arquivo: " + ex.getMessage(), ex);
        }

        Asset asset = assetRepository.save(Asset.builder()
                .organizacaoId(organizacaoId)
                .usuarioId(usuarioId)
                .tipo(TipoAsset.deOuOutro(tipo).name())
                .nomeOriginal(nomeOriginal)
                .storagePath(storagePath)
                .mimeType(arquivo.getContentType())
                .tamanhoBytes(tamanho)
                .publico(publico)
                .build());

        auditoriaService.registrar(organizacaoId, usuarioId, "ASSET_ENVIADO",
                "Asset", asset.getId(), null, null, nomeOriginal);

        return AssetResponse.de(asset);
    }

    public Conteudo baixar(Long id) {
        Asset asset = carregar(id);
        Path arquivo = uploadsRoot.resolve(asset.getStoragePath()).normalize();
        if (!arquivo.startsWith(uploadsRoot)) {
            throw new IllegalArgumentException("Caminho de asset invalido");
        }
        try {
            byte[] dados = Files.readAllBytes(arquivo);
            return new Conteudo(asset.getMimeType(), asset.getNomeOriginal(), dados);
        } catch (IOException ex) {
            throw new NotFoundException("Conteudo do asset " + id + " indisponivel");
        }
    }

    @Transactional
    public void apagar(Long id) {
        Asset asset = carregar(id);
        try {
            Files.deleteIfExists(uploadsRoot.resolve(asset.getStoragePath()).normalize());
        } catch (IOException ignored) {
            // arquivo ja removido: segue para apagar o registro
        }
        assetRepository.delete(asset);
        auditoriaService.registrar(asset.getOrganizacaoId(), asset.getUsuarioId(), "ASSET_REMOVIDO",
                "Asset", id, null, asset.getNomeOriginal(), null);
    }

    private Asset carregar(Long id) {
        return assetRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Asset " + id + " nao encontrado"));
    }

    /** Conteudo binario de um asset, para download. */
    public record Conteudo(String mimeType, String nomeOriginal, byte[] dados) {}
}
