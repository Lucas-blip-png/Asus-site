package com.asus.platform.service;

import com.asus.platform.domain.Compra;
import com.asus.platform.domain.MarketplaceItem;
import com.asus.platform.repository.CompraRepository;
import com.asus.platform.repository.MarketplaceItemRepository;
import com.asus.platform.repository.UsuarioRepository;
import com.asus.platform.web.NotFoundException;
import com.asus.platform.web.dto.CompraResponse;
import com.asus.platform.web.dto.CriarItemMarketplaceRequest;
import com.asus.platform.web.dto.MarketplaceItemResponse;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Marketplace: itens publicados e compras (plano, Seção 14 e 21.7 / Fase 12). */
@Service
public class MarketplaceService {

    private final MarketplaceItemRepository itemRepository;
    private final CompraRepository compraRepository;
    private final UsuarioRepository usuarioRepository;
    private final AuditoriaService auditoriaService;

    public MarketplaceService(MarketplaceItemRepository itemRepository,
                              CompraRepository compraRepository,
                              UsuarioRepository usuarioRepository,
                              AuditoriaService auditoriaService) {
        this.itemRepository = itemRepository;
        this.compraRepository = compraRepository;
        this.usuarioRepository = usuarioRepository;
        this.auditoriaService = auditoriaService;
    }

    public List<MarketplaceItemResponse> listar() {
        return itemRepository.findByPublicadoTrueOrderByCriadoEmDesc().stream()
                .map(MarketplaceItemResponse::de).toList();
    }

    public MarketplaceItemResponse buscar(Long id) {
        return MarketplaceItemResponse.de(carregar(id));
    }

    @Transactional
    public MarketplaceItemResponse criar(CriarItemMarketplaceRequest req) {
        boolean gratuito = req.preco() == null || req.preco().signum() <= 0;
        MarketplaceItem item = itemRepository.save(MarketplaceItem.builder()
                .autorUsuarioId(req.autorUsuarioId())
                .titulo(req.titulo())
                .descricao(req.descricao())
                .tipo(req.tipo())
                .preco(gratuito ? null : req.preco())
                .moeda(req.moeda())
                .gratuito(gratuito)
                .publicado(req.publicado() == null || req.publicado())
                .build());

        auditoriaService.registrar(null, req.autorUsuarioId(), "ITEM_MARKETPLACE_CRIADO",
                "MarketplaceItem", item.getId(), null, null, item.getTitulo());

        return MarketplaceItemResponse.de(item);
    }

    @Transactional
    public CompraResponse comprar(Long itemId, Long usuarioId) {
        MarketplaceItem item = carregar(itemId);
        if (!item.isPublicado()) {
            throw new IllegalArgumentException("Item nao esta publicado");
        }
        if (!usuarioRepository.existsById(usuarioId)) {
            throw new NotFoundException("Usuario " + usuarioId + " nao encontrado");
        }

        // Idempotente: ja comprou -> devolve a compra existente.
        Optional<Compra> existente =
                compraRepository.findByUsuarioIdAndMarketplaceItemId(usuarioId, itemId);
        if (existente.isPresent()) {
            return CompraResponse.de(existente.get());
        }

        BigDecimal valor = item.isGratuito() || item.getPreco() == null
                ? BigDecimal.ZERO : item.getPreco();
        Compra compra = compraRepository.save(Compra.builder()
                .usuarioId(usuarioId)
                .marketplaceItemId(itemId)
                .valorPago(valor)
                .moeda(item.getMoeda())
                .build());

        auditoriaService.registrar(null, usuarioId, "COMPRA_REALIZADA",
                "MarketplaceItem", itemId, "valorPago", null, String.valueOf(valor));

        return CompraResponse.de(compra);
    }

    private MarketplaceItem carregar(Long id) {
        return itemRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Item " + id + " nao encontrado"));
    }
}
