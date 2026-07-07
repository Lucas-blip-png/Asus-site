package com.asus.platform.service;

import com.asus.platform.domain.Asset;
import com.asus.platform.domain.LimitesPlano;
import com.asus.platform.domain.TipoAsset;
import com.asus.platform.repository.AssetRepository;
import com.asus.platform.service.storage.ArmazenamentoAssets;
import com.asus.platform.web.LimiteExcedidoException;
import com.asus.platform.web.NotFoundException;
import com.asus.platform.web.dto.AssetResponse;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Base64;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

/**
 * Upload/listagem/download/exclusao de assets (plano, Seção 13 / Fase 11).
 *
 * <p>A gravação física é delegada a {@link ArmazenamentoAssets} — local por padrão,
 * S3/R2/GCS via {@code asus.storage.tipo=s3} (ver {@code INTEGRACOES.md}).</p>
 */
@Service
public class AssetService {

    private final AssetRepository assetRepository;
    private final OrganizacaoService organizacaoService;
    private final PlanoService planoService;
    private final AuditoriaService auditoriaService;
    private final ArmazenamentoAssets armazenamento;
    private final DonoService donoService;

    public AssetService(AssetRepository assetRepository,
                        OrganizacaoService organizacaoService,
                        PlanoService planoService,
                        AuditoriaService auditoriaService,
                        ArmazenamentoAssets armazenamento,
                        DonoService donoService) {
        this.assetRepository = assetRepository;
        this.organizacaoService = organizacaoService;
        this.planoService = planoService;
        this.auditoriaService = auditoriaService;
        this.armazenamento = armazenamento;
        this.donoService = donoService;
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

        // Validacao de tipo (plano, Seção 20.1): imagens, PDF, texto ou JSON.
        String mime = arquivo.getContentType();
        if (mime != null && !(mime.startsWith("image/") || mime.equals("application/pdf")
                || mime.startsWith("text/") || mime.equals("application/json"))) {
            throw new IllegalArgumentException("Tipo de arquivo nao permitido: " + mime);
        }

        long tamanho = arquivo.getSize();
        LimitesPlano limites = planoService.limitesDa(organizacaoId);
        long usado = assetRepository.somaBytesPorOrganizacao(organizacaoId);
        if (usado + tamanho > limites.assetsBytesMax()) {
            throw new LimiteExcedidoException("Cota de assets do plano excedida");
        }

        String nomeOriginal = StringUtils.cleanPath(
                arquivo.getOriginalFilename() == null ? "arquivo" : arquivo.getOriginalFilename());

        // Le os bytes uma vez: vao tanto pro storage (compat) quanto pro banco (persistencia).
        byte[] bytes;
        String storagePath;
        try {
            bytes = arquivo.getBytes();
            try (InputStream in = new ByteArrayInputStream(bytes)) {
                storagePath = armazenamento.gravar(organizacaoId, nomeOriginal, in, tamanho);
            }
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
                .dadosBase64(Base64.getEncoder().encodeToString(bytes))
                .publico(publico)
                .build());

        auditoriaService.registrar(organizacaoId, usuarioId, "ASSET_ENVIADO",
                "Asset", asset.getId(), null, null, nomeOriginal);

        return AssetResponse.de(asset);
    }

    public Conteudo baixar(Long id) {
        Asset asset = carregar(id);
        // Preferencia: bytes do banco (persistem). Fallback: storage local (assets antigos).
        if (asset.getDadosBase64() != null) {
            byte[] dados = Base64.getDecoder().decode(asset.getDadosBase64());
            return new Conteudo(asset.getMimeType(), asset.getNomeOriginal(), dados);
        }
        try {
            byte[] dados = armazenamento.ler(asset.getStoragePath());
            return new Conteudo(asset.getMimeType(), asset.getNomeOriginal(), dados);
        } catch (IOException ex) {
            throw new NotFoundException("Conteudo do asset " + id + " indisponivel");
        }
    }

    @Transactional
    public void apagar(Long id, com.asus.platform.security.UsuarioPrincipal principal) {
        Asset asset = carregar(id);
        // So o dono do upload (ou o dono/dev) pode apagar; assets antigos sem dono ficam livres.
        if (principal != null && asset.getUsuarioId() != null
                && !asset.getUsuarioId().equals(principal.id())
                && !donoService.ehDono(principal.id())) {
            throw new com.asus.platform.web.AcessoNegadoException("Este asset pertence a outro usuário.");
        }
        armazenamento.apagar(asset.getStoragePath());
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
