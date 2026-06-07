package com.asus.platform.web;

import com.asus.platform.service.MarketplaceService;
import com.asus.platform.web.dto.CompraResponse;
import com.asus.platform.web.dto.ComprarRequest;
import com.asus.platform.web.dto.CriarItemMarketplaceRequest;
import com.asus.platform.web.dto.MarketplaceItemResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

/** Marketplace (plano, Seção 21.7). */
@RestController
@RequestMapping("/api/marketplace")
public class MarketplaceController {

    private final MarketplaceService service;

    public MarketplaceController(MarketplaceService service) {
        this.service = service;
    }

    @GetMapping
    public List<MarketplaceItemResponse> listar() {
        return service.listar();
    }

    @PostMapping("/items")
    @ResponseStatus(HttpStatus.CREATED)
    public MarketplaceItemResponse criar(@Valid @RequestBody CriarItemMarketplaceRequest req) {
        return service.criar(req);
    }

    @GetMapping("/items/{id}")
    public MarketplaceItemResponse buscar(@PathVariable Long id) {
        return service.buscar(id);
    }

    @PostMapping("/items/{id}/comprar")
    @ResponseStatus(HttpStatus.CREATED)
    public CompraResponse comprar(@PathVariable Long id, @Valid @RequestBody ComprarRequest req) {
        return service.comprar(id, req.usuarioId());
    }
}
