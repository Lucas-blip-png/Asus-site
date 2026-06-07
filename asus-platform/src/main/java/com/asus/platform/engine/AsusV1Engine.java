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
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * Motor de regras ASUS V1 (plano, secao 5.2).
 *
 * <p>Formulas ASUS_V1 (padrao, ajustaveis sem quebrar fichas antigas):</p>
 * <ul>
 *   <li>atributosFinais = atributosBase + bonus raciais (raca.jsonHabilidades)</li>
 *   <li>PV  = raca.pvBase + classe.multiplicadorPv * nivel + vigor    * 2</li>
 *   <li>PM  = raca.pmBase + classe.multiplicadorPm * nivel + intelecto * 2</li>
 *   <li>PE  = raca.peBase + classe.multiplicadorPe * nivel + agilidade</li>
 *   <li>Defesa = 10 + agilidade</li>
 *   <li>Pericia = atributoFinal(atributoBase) + (nivel / 2)</li>
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
        Classe classe = ctx.classe();
        int nivel = Math.max(1, ctx.nivel());

        // 1) Atributos finais = base + bonus raciais
        Atributos finais = copiar(ctx.atributosBase());
        passos.add("Atributos base: " + descreverAtributos(finais));

        Map<Atributo, Integer> bonus = lerBonusRaciais(raca.getJsonHabilidades());
        for (Map.Entry<Atributo, Integer> e : bonus.entrySet()) {
            finais = finais.comBonus(e.getKey(), e.getValue());
            passos.add(String.format("Bonus racial (%s): %s %+d",
                    raca.getNome(), e.getKey(), e.getValue()));
        }
        passos.add("Atributos finais: " + descreverAtributos(finais));

        // 2) Status derivado
        int pv = raca.getPvBase() + classe.getMultiplicadorPv() * nivel + finais.getVigor() * 2;
        passos.add(String.format(
                "PV = pvBase(%d) + multPv(%d) * nivel(%d) + vigor(%d) * 2 = %d",
                raca.getPvBase(), classe.getMultiplicadorPv(), nivel, finais.getVigor(), pv));

        int pm = raca.getPmBase() + classe.getMultiplicadorPm() * nivel + finais.getIntelecto() * 2;
        passos.add(String.format(
                "PM = pmBase(%d) + multPm(%d) * nivel(%d) + intelecto(%d) * 2 = %d",
                raca.getPmBase(), classe.getMultiplicadorPm(), nivel, finais.getIntelecto(), pm));

        int pe = raca.getPeBase() + classe.getMultiplicadorPe() * nivel + finais.getAgilidade();
        passos.add(String.format(
                "PE = peBase(%d) + multPe(%d) * nivel(%d) + agilidade(%d) = %d",
                raca.getPeBase(), classe.getMultiplicadorPe(), nivel, finais.getAgilidade(), pe));

        int defesa = 10 + finais.getAgilidade();
        passos.add(String.format("Defesa = 10 + agilidade(%d) = %d", finais.getAgilidade(), defesa));

        Status status = Status.builder()
                .pvMax(pv).pvAtual(pv)
                .pmMax(pm).pmAtual(pm)
                .peMax(pe).peAtual(pe)
                .defesa(defesa)
                .build();

        // 3) Pericias
        int bonusNivel = nivel / 2;
        List<PericiaCalculada> pericias = new ArrayList<>();
        for (Pericia p : ctx.periciasDoSistema()) {
            Atributo attr = Atributo.valueOf(p.getAtributoBase().toUpperCase(Locale.ROOT));
            int valor = finais.get(attr) + bonusNivel;
            pericias.add(new PericiaCalculada(p.getCodigo(), p.getNome(), attr.name(), valor));
            passos.add(String.format("Pericia %s = %s(%d) + nivel/2(%d) = %d",
                    p.getNome(), attr, finais.get(attr), bonusNivel, valor));
        }

        return new ResultadoCalculo(finais, status, pericias, passos);
    }

    private Map<Atributo, Integer> lerBonusRaciais(String json) {
        Map<Atributo, Integer> resultado = new java.util.EnumMap<>(Atributo.class);
        if (json == null || json.isBlank()) {
            return resultado;
        }
        try {
            JsonNode raiz = objectMapper.readTree(json);
            JsonNode bonusNode = raiz.get("bonusAtributos");
            if (bonusNode != null && bonusNode.isObject()) {
                Iterator<Map.Entry<String, JsonNode>> campos = bonusNode.fields();
                while (campos.hasNext()) {
                    Map.Entry<String, JsonNode> campo = campos.next();
                    try {
                        Atributo attr = Atributo.valueOf(campo.getKey().toUpperCase(Locale.ROOT));
                        resultado.put(attr, campo.getValue().asInt());
                    } catch (IllegalArgumentException ignored) {
                        // atributo desconhecido: ignora silenciosamente
                    }
                }
            }
        } catch (Exception ex) {
            // json invalido: trata como sem bonus
        }
        return resultado;
    }

    private Atributos copiar(Atributos a) {
        if (a == null) {
            return Atributos.builder().build();
        }
        return Atributos.builder()
                .forca(a.getForca())
                .agilidade(a.getAgilidade())
                .vigor(a.getVigor())
                .intelecto(a.getIntelecto())
                .presenca(a.getPresenca())
                .build();
    }

    private String descreverAtributos(Atributos a) {
        return String.format("FOR %d, AGI %d, VIG %d, INT %d, PRE %d",
                a.getForca(), a.getAgilidade(), a.getVigor(), a.getIntelecto(), a.getPresenca());
    }
}
