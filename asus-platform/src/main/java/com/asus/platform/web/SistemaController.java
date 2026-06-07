package com.asus.platform.web;

import com.asus.platform.domain.Atributo;
import com.asus.platform.domain.Classe;
import com.asus.platform.domain.GameSystem;
import com.asus.platform.domain.Habilidade;
import com.asus.platform.domain.ItemJogo;
import com.asus.platform.domain.Pericia;
import com.asus.platform.domain.ProgressaoNivel;
import com.asus.platform.domain.Raca;
import com.asus.platform.engine.AsusV1Engine;
import com.asus.platform.repository.ClasseRepository;
import com.asus.platform.repository.GameSystemRepository;
import com.asus.platform.repository.HabilidadeRepository;
import com.asus.platform.repository.ItemJogoRepository;
import com.asus.platform.repository.PericiaRepository;
import com.asus.platform.repository.ProgressaoNivelRepository;
import com.asus.platform.repository.RacaRepository;
import com.asus.platform.web.dto.AtributoInfo;
import com.asus.platform.web.dto.FeiticoRegras;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Exposicao das regras do sistema ASUS (plano, secoes 2.4 e 21.2). */
@RestController
@RequestMapping("/api/sistemas")
public class SistemaController {

    private final GameSystemRepository gameSystemRepository;
    private final RacaRepository racaRepository;
    private final ClasseRepository classeRepository;
    private final PericiaRepository periciaRepository;
    private final HabilidadeRepository habilidadeRepository;
    private final ItemJogoRepository itemJogoRepository;
    private final ProgressaoNivelRepository progressaoNivelRepository;

    public SistemaController(GameSystemRepository gameSystemRepository,
                             RacaRepository racaRepository,
                             ClasseRepository classeRepository,
                             PericiaRepository periciaRepository,
                             HabilidadeRepository habilidadeRepository,
                             ItemJogoRepository itemJogoRepository,
                             ProgressaoNivelRepository progressaoNivelRepository) {
        this.gameSystemRepository = gameSystemRepository;
        this.racaRepository = racaRepository;
        this.classeRepository = classeRepository;
        this.periciaRepository = periciaRepository;
        this.habilidadeRepository = habilidadeRepository;
        this.itemJogoRepository = itemJogoRepository;
        this.progressaoNivelRepository = progressaoNivelRepository;
    }

    @GetMapping
    public List<GameSystem> listar() {
        return gameSystemRepository.findAll();
    }

    @GetMapping("/asus")
    public GameSystem asus() {
        return asusSystem();
    }

    @GetMapping("/asus/atributos")
    public List<AtributoInfo> atributos() {
        return List.of(
                AtributoInfo.de(Atributo.FORCA, "Forca", "Potencia fisica, dano corpo a corpo e carga (2x)."),
                AtributoInfo.de(Atributo.CONSTITUICAO, "Constituicao", "+2 de vida e +2 de energia por ponto."),
                AtributoInfo.de(Atributo.DESTREZA, "Destreza", "Habilidade manual; limite de Habilidades (metade)."),
                AtributoInfo.de(Atributo.AGILIDADE, "Agilidade", "Reflexos e Deslocamento (4 + 1 a cada 5)."),
                AtributoInfo.de(Atributo.INTELIGENCIA, "Inteligencia", "Feiticaria; limite de Feiticos (metade)."),
                AtributoInfo.de(Atributo.SABEDORIA, "Sabedoria", "Instinto; limite de Bencaos (metade)."),
                AtributoInfo.de(Atributo.CARISMA, "Carisma", "Interacao social e primeira impressao."));
    }

    @GetMapping("/asus/racas")
    public List<Raca> racas() {
        return racaRepository.findByGameSystemId(asusSystem().getId());
    }

    @GetMapping("/asus/classes")
    public List<Classe> classes(@RequestParam(required = false) Boolean base) {
        List<Classe> todas = classeRepository.findByGameSystemId(asusSystem().getId());
        if (Boolean.TRUE.equals(base)) {
            return todas.stream().filter(c -> c.getClassePaiCodigo() == null).toList();
        }
        return todas;
    }

    @GetMapping("/asus/pericias")
    public List<Pericia> pericias() {
        return periciaRepository.findByGameSystemId(asusSystem().getId());
    }

    @GetMapping("/asus/habilidades")
    public List<Habilidade> habilidades(@RequestParam(required = false) String classe) {
        Long id = asusSystem().getId();
        return classe == null
                ? habilidadeRepository.findByGameSystemId(id)
                : habilidadeRepository.findByGameSystemIdAndClasseCodigo(id, classe);
    }

    @GetMapping("/asus/itens")
    public List<ItemJogo> itens(@RequestParam(required = false) String categoria) {
        Long id = asusSystem().getId();
        return categoria == null
                ? itemJogoRepository.findByGameSystemId(id)
                : itemJogoRepository.findByGameSystemIdAndCategoria(id, categoria);
    }

    @GetMapping("/asus/progressao")
    public List<ProgressaoNivel> progressao() {
        return progressaoNivelRepository.findByGameSystemIdOrderByNivel(asusSystem().getId());
    }

    @GetMapping("/asus/feiticos/regras")
    public FeiticoRegras feiticosRegras() {
        return FeiticoRegras.padrao();
    }

    private GameSystem asusSystem() {
        return gameSystemRepository.findByCodigo(AsusV1Engine.SYSTEM_ID)
                .orElseThrow(() -> new NotFoundException("Sistema ASUS nao encontrado"));
    }
}
