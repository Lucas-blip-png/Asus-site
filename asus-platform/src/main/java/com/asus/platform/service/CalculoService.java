package com.asus.platform.service;

import com.asus.platform.domain.Atributos;
import com.asus.platform.domain.Classe;
import com.asus.platform.domain.GameSystem;
import com.asus.platform.domain.Personagem;
import com.asus.platform.domain.Pericia;
import com.asus.platform.domain.Raca;
import com.asus.platform.engine.ContextoCalculo;
import com.asus.platform.engine.GameSystemEngine;
import com.asus.platform.engine.GameSystemRegistry;
import com.asus.platform.engine.ResultadoCalculo;
import com.asus.platform.repository.ClasseRepository;
import com.asus.platform.repository.GameSystemRepository;
import com.asus.platform.repository.PericiaRepository;
import com.asus.platform.repository.RacaRepository;
import com.asus.platform.web.NotFoundException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Service;

/**
 * Calcula a ficha de um personagem. O backend e a fonte unica das regras:
 * resolve raca, classe, trilha e pericias e delega ao engine correto.
 */
@Service
public class CalculoService {

    private final GameSystemRepository gameSystemRepository;
    private final RacaRepository racaRepository;
    private final ClasseRepository classeRepository;
    private final PericiaRepository periciaRepository;
    private final GameSystemRegistry registry;
    private final ObjectMapper objectMapper;

    public CalculoService(GameSystemRepository gameSystemRepository,
                          RacaRepository racaRepository,
                          ClasseRepository classeRepository,
                          PericiaRepository periciaRepository,
                          GameSystemRegistry registry,
                          ObjectMapper objectMapper) {
        this.gameSystemRepository = gameSystemRepository;
        this.racaRepository = racaRepository;
        this.classeRepository = classeRepository;
        this.periciaRepository = periciaRepository;
        this.registry = registry;
        this.objectMapper = objectMapper;
    }

    public ResultadoCalculo calcular(Personagem personagem) {
        return calcular(personagem, 0);
    }

    /**
     * Recalcula com uma penalidade de Agilidade (ex.: sobrecarga de inventario:
     * cada ponto acima da carga maxima tira 1 de Agilidade). A penalidade entra na
     * base, entao cascateia para deslocamento, pericias de AGI, etc.
     */
    public ResultadoCalculo calcular(Personagem personagem, int penalidadeAgilidade) {
        GameSystem sistema = gameSystemRepository.findById(personagem.getGameSystemId())
                .orElseThrow(() -> new NotFoundException("Sistema nao encontrado"));

        Raca raca = racaRepository.findById(personagem.getRacaId())
                .orElseThrow(() -> new NotFoundException("Raca nao encontrada"));

        Classe classe = classeRepository.findById(personagem.getClasseId())
                .orElseThrow(() -> new NotFoundException("Classe nao encontrada"));

        List<Classe> fontes = new ArrayList<>();
        fontes.add(classe);
        if (personagem.getTrilhaId() != null) {
            classeRepository.findById(personagem.getTrilhaId()).ifPresent(fontes::add);
        }

        List<Pericia> pericias = periciaRepository.findByGameSystemId(sistema.getId());
        Map<String, Integer> treino = parseTreino(personagem.getJsonPericias());

        GameSystemEngine engine = registry.getEngine(sistema.getCodigo(), personagem.getRulesetVersion());

        Atributos base = personagem.getAtributosBase();
        if (penalidadeAgilidade > 0 && base != null) {
            base = Atributos.builder()
                    .forca(base.getForca()).constituicao(base.getConstituicao()).destreza(base.getDestreza())
                    .agilidade(base.getAgilidade() - penalidadeAgilidade)
                    .inteligencia(base.getInteligencia()).sabedoria(base.getSabedoria()).carisma(base.getCarisma())
                    .build();
        }

        ContextoCalculo contexto = new ContextoCalculo(
                raca, fontes, base, personagem.getNivel(), pericias, treino);

        return engine.calcular(contexto);
    }

    private Map<String, Integer> parseTreino(String json) {
        Map<String, Integer> treino = new HashMap<>();
        if (json == null || json.isBlank()) {
            return treino;
        }
        try {
            JsonNode raiz = objectMapper.readTree(json);
            if (raiz.isObject()) {
                Iterator<Map.Entry<String, JsonNode>> it = raiz.fields();
                while (it.hasNext()) {
                    Map.Entry<String, JsonNode> e = it.next();
                    treino.put(e.getKey().toUpperCase(Locale.ROOT), e.getValue().asInt());
                }
            }
        } catch (Exception ignored) {
            // json invalido: trata como sem treino
        }
        return treino;
    }
}
