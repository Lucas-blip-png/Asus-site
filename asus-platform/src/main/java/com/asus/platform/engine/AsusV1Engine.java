package com.asus.platform.engine;

import com.asus.platform.domain.Atributo;
import com.asus.platform.domain.Atributos;
import com.asus.platform.domain.Classe;
import com.asus.platform.domain.Pericia;
import com.asus.platform.domain.Raca;
import com.asus.platform.domain.Status;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * Motor de regras ASUS V1 — fiel ao livro "Asus - Projeto de Sistema".
 *
 * <p>Fonte dos valores: a raca fornece a base de PV/PM/PE; as classes/trilhas
 * fornecem os bonus de atributos e o treino inicial de pericias.</p>
 *
 * <ul>
 *   <li>atributosFinais = atributosBase + bonus de classe/trilha</li>
 *   <li>PV  = raca.pvBase + Constituicao * 2  (Con da +2 de vida por ponto)</li>
 *   <li>PM  = raca.pmBase + Inteligencia * 2  (Int conduz a feiticaria)</li>
 *   <li>PE  = raca.peBase + Constituicao * 2  (Con da +2 de energia por ponto)</li>
 *   <li>Deslocamento = 4 + Agilidade / 5</li>
 *   <li>Carga maxima = Forca * 2</li>
 *   <li>Limites: Habilidades = Des/2 · Feiticos = Int/2 · Bencaos = Sab/2</li>
 *   <li>Pericia: teste = 1d20 + treino; treino vai ate 2x o atributo (cap)</li>
 * </ul>
 */
@Component
public class AsusV1Engine implements GameSystemEngine {

    public static final String SYSTEM_ID = "ASUS";
    public static final String VERSION = "ASUS_V1";

    /** Regra de rolagem ASUS_V1: d20 natural 20 e acerto critico, 1 e falha critica. */
    public static final int D20_CRITICO = 20;
    public static final int D20_FALHA_CRITICA = 1;

    private final ObjectMapper objectMapper;

    public AsusV1Engine(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public String getSystemId() {
        return SYSTEM_ID;
    }

    @Override
    public String getVersion() {
        return VERSION;
    }

    @Override
    public ResultadoCalculo calcular(ContextoCalculo ctx) {
        List<String> passos = new ArrayList<>();
        Raca raca = ctx.raca();

        Atributos finais = ctx.atributosBase() == null
                ? Atributos.builder().build() : ctx.atributosBase().copia();
        passos.add("Atributos base: " + descrever(finais));

        Map<String, Integer> treino = new HashMap<>();
        if (ctx.periciasTreino() != null) {
            ctx.periciasTreino().forEach((k, v) -> treino.put(k.toUpperCase(Locale.ROOT), v));
        }

        // Bonus de classe e trilha (PV/PM/PE da classe + atributos + treino de pericias)
        List<Classe> fontes = ctx.fontesBonus() == null ? List.of() : ctx.fontesBonus();
        int classePv = 0;
        int classePm = 0;
        int classePe = 0;
        for (Classe c : fontes) {
            if (c == null) {
                continue;
            }
            classePv += c.getMultiplicadorPv();
            classePm += c.getMultiplicadorPm();
            classePe += c.getMultiplicadorPe();
            JsonNode bonus = lerJson(c.getJsonBonus());
            if (bonus == null) {
                continue;
            }
            JsonNode atrs = bonus.get("atributos");
            if (atrs != null && atrs.isObject()) {
                Iterator<Map.Entry<String, JsonNode>> it = atrs.fields();
                while (it.hasNext()) {
                    Map.Entry<String, JsonNode> e = it.next();
                    Atributo a = atributo(e.getKey());
                    if (a != null) {
                        finais = finais.comBonus(a, e.getValue().asInt());
                        passos.add(String.format("Bonus de %s: %s %+d", c.getNome(), a, e.getValue().asInt()));
                    }
                }
            }
            JsonNode pers = bonus.get("pericias");
            if (pers != null && pers.isObject()) {
                Iterator<Map.Entry<String, JsonNode>> it = pers.fields();
                while (it.hasNext()) {
                    Map.Entry<String, JsonNode> e = it.next();
                    treino.merge(e.getKey().toUpperCase(Locale.ROOT), e.getValue().asInt(), Integer::sum);
                }
            }
        }
        passos.add("Atributos finais: " + descrever(finais));

        // Status derivado
        int pv = raca.getPvBase() + classePv + finais.getConstituicao() * 2;
        int pm = raca.getPmBase() + classePm + finais.getInteligencia() * 2;
        int pe = raca.getPeBase() + classePe + finais.getConstituicao() * 2;
        passos.add(String.format("PV = racaPV(%d) + classePV(%d) + Con(%d)x2 = %d",
                raca.getPvBase(), classePv, finais.getConstituicao(), pv));
        passos.add(String.format("PM = racaPM(%d) + classePM(%d) + Int(%d)x2 = %d",
                raca.getPmBase(), classePm, finais.getInteligencia(), pm));
        passos.add(String.format("PE = racaPE(%d) + classePE(%d) + Con(%d)x2 = %d",
                raca.getPeBase(), classePe, finais.getConstituicao(), pe));

        Status status = Status.builder()
                .pvMax(pv).pvAtual(pv)
                .pmMax(pm).pmAtual(pm)
                .peMax(pe).peAtual(pe)
                .build();

        int deslocamento = 4 + finais.getAgilidade() / 5;
        int carga = finais.getForca() * 2;
        int limHab = finais.getDestreza() / 2;
        int limFei = finais.getInteligencia() / 2;
        int limBen = finais.getSabedoria() / 2;
        passos.add(String.format("Deslocamento = 4 + Agi(%d)/5 = %d", finais.getAgilidade(), deslocamento));
        passos.add(String.format("Carga maxima = For(%d)x2 = %d", finais.getForca(), carga));
        passos.add(String.format("Limites -> Habilidades %d, Feiticos %d, Bencaos %d", limHab, limFei, limBen));

        // Pericias: valor treinado e cap (2x atributo final)
        List<PericiaCalculada> pericias = new ArrayList<>();
        for (Pericia p : ctx.periciasDoSistema()) {
            Atributo a = atributo(p.getAtributoBase());
            if (a == null) {
                continue;
            }
            int cap = Math.max(0, finais.get(a) * 2);
            int t = Math.max(0, Math.min(treino.getOrDefault(p.getCodigo().toUpperCase(Locale.ROOT), 0), cap));
            pericias.add(new PericiaCalculada(p.getCodigo(), p.getNome(), a.name(), a.getSigla(), t, cap));
        }

        return new ResultadoCalculo(finais, status, pericias,
                deslocamento, carga, limHab, limFei, limBen, passos);
    }

    private Atributo atributo(String nome) {
        if (nome == null) {
            return null;
        }
        try {
            return Atributo.valueOf(nome.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private JsonNode lerJson(String json) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readTree(json);
        } catch (Exception e) {
            return null;
        }
    }

    private String descrever(Atributos a) {
        return String.format("For %d, Con %d, Des %d, Agi %d, Int %d, Sab %d, Car %d",
                a.getForca(), a.getConstituicao(), a.getDestreza(), a.getAgilidade(),
                a.getInteligencia(), a.getSabedoria(), a.getCarisma());
    }
}
