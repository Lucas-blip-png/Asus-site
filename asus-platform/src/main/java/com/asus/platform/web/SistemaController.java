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
import java.util.Map;
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
        List<Habilidade> lista = classe == null
                ? habilidadeRepository.findByGameSystemId(id)
                : habilidadeRepository.findByGameSystemIdAndClasseCodigo(id, classe);
        // Nao expoe as habilidades "proprias" (custom por personagem) no catalogo do Livro.
        return lista.stream().filter(h -> !"PROPRIA".equals(h.getClasseCodigo())).toList();
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

    /** Feiticos prontos (montados pelas regras de construcao) para adicionar com 1 clique. */
    @GetMapping("/asus/feiticos/prontos")
    public List<Map<String, Object>> feiticosProntos() {
        return List.of(
                feitico("Míssil Arcano", 1, 2, "9m", "Instantânea", "Projétil de energia que causa 1d6 de dano arcano em um alvo."),
                feitico("Toque Curativo", 1, 2, "Toque", "Instantânea", "Cura 1d6 PV de uma criatura tocada."),
                feitico("Luz", 1, 1, "Toque", "1 cena", "Um objeto tocado brilha como uma tocha."),
                feitico("Armadura Arcana", 1, 2, "Pessoal", "1 cena", "+2 de Defesa enquanto durar."),
                feitico("Susto", 1, 2, "9m", "Instantânea", "O alvo faz teste de Sabedoria ou fica Amedrontado por 1 rodada."),
                feitico("Bola de Fogo", 2, 5, "18m", "Instantânea", "Explosão em área de 3m que causa 3d6 de dano de fogo."),
                feitico("Cura Moderada", 2, 5, "Toque", "Instantânea", "Cura 3d6 PV de uma criatura tocada."),
                feitico("Invisibilidade", 2, 6, "Pessoal", "1 cena ou até atacar", "Você fica invisível."),
                feitico("Levitar", 2, 4, "Pessoal", "1 cena", "Você flutua a até 3m do chão, movendo-se lentamente."),
                feitico("Relâmpago", 3, 8, "18m", "Instantânea", "Linha de raio que causa 5d6 de dano elétrico nos alvos na trajetória."),
                feitico("Cura Superior", 3, 8, "Toque", "Instantânea", "Cura 5d6 PV de uma criatura tocada."),
                feitico("Voo", 3, 9, "Pessoal", "1 cena", "Você voa com deslocamento igual ao seu andar."),
                feitico("Muralha de Gelo", 3, 8, "12m", "1 cena", "Parede de gelo de 6m que bloqueia passagem e visão."),
                feitico("Tempestade Flamejante", 4, 12, "18m", "Instantânea", "Área de 6m em chamas: 7d6 de dano de fogo."),
                feitico("Reviver", 4, 14, "Toque", "Instantânea", "Estabiliza e devolve 1 PV a uma criatura que morreu nesta cena."),
                feitico("Palavra do Poder", 5, 18, "9m", "Instantânea", "Uma palavra devastadora: 10d6 de dano em um único alvo."));
    }

    private static Map<String, Object> feitico(String nome, int circulo, int custoPm,
                                               String alcance, String duracao, String efeito) {
        Map<String, Object> m = new java.util.LinkedHashMap<>();
        m.put("nome", nome);
        m.put("circulo", circulo);
        m.put("custoPm", custoPm);
        m.put("alcance", alcance);
        m.put("duracao", duracao);
        m.put("efeito", efeito);
        return m;
    }

    private GameSystem asusSystem() {
        return gameSystemRepository.findByCodigo(AsusV1Engine.SYSTEM_ID)
                .orElseThrow(() -> new NotFoundException("Sistema ASUS nao encontrado"));
    }
}
