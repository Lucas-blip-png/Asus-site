package com.asus.platform.web;

import com.asus.platform.domain.Ataque;
import com.asus.platform.domain.GameSystem;
import com.asus.platform.domain.ItemJogo;
import com.asus.platform.domain.ItemPersonagem;
import com.asus.platform.engine.AsusV1Engine;
import com.asus.platform.repository.AtaqueRepository;
import com.asus.platform.repository.GameSystemRepository;
import com.asus.platform.repository.ItemJogoRepository;
import com.asus.platform.repository.ItemPersonagemRepository;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

/** Inventario do personagem (aba Inventário): catalogo T20 + itens proprios, respeitando carga. */
@RestController
@RequestMapping("/api")
public class InventarioController {

    private final ItemPersonagemRepository inventarioRepository;
    private final ItemJogoRepository itemJogoRepository;
    private final GameSystemRepository gameSystemRepository;
    private final AtaqueRepository ataqueRepository;

    public InventarioController(ItemPersonagemRepository inventarioRepository,
                                ItemJogoRepository itemJogoRepository,
                                GameSystemRepository gameSystemRepository,
                                AtaqueRepository ataqueRepository) {
        this.inventarioRepository = inventarioRepository;
        this.itemJogoRepository = itemJogoRepository;
        this.gameSystemRepository = gameSystemRepository;
        this.ataqueRepository = ataqueRepository;
    }

    @GetMapping("/personagens/{id}/inventario")
    public List<ItemPersonagem> listar(@PathVariable Long id) {
        return inventarioRepository.findByPersonagemId(id);
    }

    /** Adiciona um item proprio (custom). */
    @PostMapping("/personagens/{id}/inventario")
    @ResponseStatus(HttpStatus.CREATED)
    public ItemPersonagem adicionar(@PathVariable Long id, @RequestBody ItemPersonagem item) {
        if (item.getNome() == null || item.getNome().isBlank()) {
            throw new IllegalArgumentException("nome do item e obrigatorio");
        }
        item.setId(null);
        item.setPersonagemId(id);
        if (item.getQuantidade() == null || item.getQuantidade() < 1) {
            item.setQuantidade(1);
        }
        if (item.getEspacos() == null) {
            item.setEspacos(0.0);
        }
        ItemPersonagem salvo = inventarioRepository.save(item);
        garantirAtaque(salvo);
        return salvo;
    }

    /** Adiciona ao inventario uma copia de um item do catalogo (por codigo). */
    @PostMapping("/personagens/{id}/inventario/do-catalogo/{codigo}")
    @ResponseStatus(HttpStatus.CREATED)
    public ItemPersonagem doCatalogo(@PathVariable Long id, @PathVariable String codigo) {
        ItemJogo c = itemJogoRepository.findByGameSystemIdAndCodigo(asus(), codigo)
                .orElseThrow(() -> new NotFoundException("Item '" + codigo + "' nao encontrado no catalogo"));
        ItemPersonagem item = ItemPersonagem.builder()
                .personagemId(id).nome(c.getNome()).categoria(c.getCategoria())
                .espacos(c.getEspacos() == null ? 0.0 : c.getEspacos()).quantidade(1).equipado(false)
                .dano(c.getDano()).critico(c.getCritico()).alcance(c.getAlcance()).tipoDano(c.getTipoDano())
                .bonusDefesa(c.getBonusDefesa()).penalidade(c.getPenalidade())
                .preco(c.getPreco()).moeda(c.getMoeda()).efeito(c.getEfeito()).itemJogoCodigo(c.getCodigo())
                .build();
        ItemPersonagem salvo = inventarioRepository.save(item);
        garantirAtaque(salvo);
        return salvo;
    }

    /** Envia um item do inventario para a aba Combate (cria o ataque; idempotente por nome). */
    @PostMapping("/inventario/{itemId}/para-combate")
    @ResponseStatus(HttpStatus.CREATED)
    public Ataque paraCombate(@PathVariable Long itemId) {
        ItemPersonagem item = inventarioRepository.findById(itemId)
                .orElseThrow(() -> new NotFoundException("Item de inventario " + itemId + " nao encontrado"));
        Ataque a = garantirAtaque(item);
        if (a == null) {
            throw new IllegalArgumentException("Este item nao e uma arma (defina o dano antes de enviar pro combate).");
        }
        return a;
    }

    /**
     * Garante que exista um ataque na aba Combate para uma arma (item com dano):
     * devolve o ataque de mesmo nome se ja existir, senao cria. Retorna null se o item nao tem dano.
     */
    private Ataque garantirAtaque(ItemPersonagem item) {
        if (item.getDano() == null || item.getDano().isBlank()) {
            return null;
        }
        return ataqueRepository.findByPersonagemId(item.getPersonagemId()).stream()
                .filter(a -> a.getNome() != null && a.getNome().equalsIgnoreCase(item.getNome()))
                .findFirst()
                .orElseGet(() -> ataqueRepository.save(Ataque.builder()
                        .personagemId(item.getPersonagemId())
                        .nome(item.getNome())
                        .dano(item.getDano())
                        .critico(item.getCritico())
                        .alcance(item.getAlcance())
                        .efeito(item.getEfeito())
                        .build()));
    }

    @PutMapping("/inventario/{itemId}")
    public ItemPersonagem atualizar(@PathVariable Long itemId, @RequestBody ItemPersonagem patch) {
        ItemPersonagem item = inventarioRepository.findById(itemId)
                .orElseThrow(() -> new NotFoundException("Item de inventario " + itemId + " nao encontrado"));
        if (patch.getQuantidade() != null) {
            item.setQuantidade(Math.max(0, patch.getQuantidade()));
        }
        if (patch.getEspacos() != null) {
            item.setEspacos(Math.max(0.0, patch.getEspacos()));
        }
        item.setEquipado(patch.isEquipado());
        if (patch.getNome() != null && !patch.getNome().isBlank()) {
            item.setNome(patch.getNome());
        }
        // Demais campos editaveis (inclusive de itens vindos do catalogo: editam a copia da ficha)
        if (patch.getCategoria() != null) item.setCategoria(patch.getCategoria());
        if (patch.getDano() != null) item.setDano(patch.getDano());
        if (patch.getCritico() != null) item.setCritico(patch.getCritico());
        if (patch.getAlcance() != null) item.setAlcance(patch.getAlcance());
        if (patch.getTipoDano() != null) item.setTipoDano(patch.getTipoDano());
        if (patch.getBonusDefesa() != null) item.setBonusDefesa(patch.getBonusDefesa());
        if (patch.getPenalidade() != null) item.setPenalidade(patch.getPenalidade());
        if (patch.getEfeito() != null) {
            item.setEfeito(patch.getEfeito());
        }
        return inventarioRepository.save(item);
    }

    @DeleteMapping("/inventario/{itemId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void remover(@PathVariable Long itemId) {
        inventarioRepository.deleteById(itemId);
    }

    private Long asus() {
        return gameSystemRepository.findByCodigo(AsusV1Engine.SYSTEM_ID)
                .map(GameSystem::getId)
                .orElseThrow(() -> new NotFoundException("Sistema ASUS nao encontrado"));
    }
}
