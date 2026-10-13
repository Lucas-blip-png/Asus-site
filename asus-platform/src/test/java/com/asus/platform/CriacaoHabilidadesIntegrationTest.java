package com.asus.platform;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Habilidades na ficha: o sistema nao escolhe nenhuma sozinho na criacao (o jogador escolhe),
 * e so o Humano de nivel 20+ (Versatilidade Humana) pode ter 1 de fora da propria classe.
 */
@SpringBootTest
@AutoConfigureMockMvc
class CriacaoHabilidadesIntegrationTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    private static final String ATRIBUTOS = "\"atributosBase\":{\"forca\":0,\"constituicao\":0,\"destreza\":0,\"agilidade\":0,"
            + "\"inteligencia\":0,\"sabedoria\":0,\"carisma\":0}";

    private JsonNode ler(String url) throws Exception {
        return objectMapper.readTree(mockMvc.perform(get(url)).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString());
    }

    private long org() throws Exception {
        return objectMapper.readTree(mockMvc.perform(post("/api/organizacoes").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Mesa Hab\",\"slug\":\"mesa-hab-" + System.nanoTime() + "\"}"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString()).get("id").asLong();
    }

    /** A criacao e sempre no nivel 1; os demais niveis vem pelo /progresso. */
    private long criar(long org, String raca, String classe, int nivel) throws Exception {
        long id = objectMapper.readTree(mockMvc.perform(post("/api/organizacoes/" + org + "/personagens")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"T\",\"racaCodigo\":\"" + raca + "\",\"classeCodigo\":\"" + classe
                                + "\",\"nivel\":1," + ATRIBUTOS + "}"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString()).get("id").asLong();
        if (nivel > 1) {
            mockMvc.perform(patch("/api/personagens/" + id + "/progresso")
                            .contentType(MediaType.APPLICATION_JSON).content("{\"nivel\":" + nivel + "}"))
                    .andExpect(status().isOk());
        }
        return id;
    }

    private int adicionar(long id, String codigo) throws Exception {
        return mockMvc.perform(post("/api/personagens/" + id + "/habilidades")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"codigo\":\"" + codigo + "\"}"))
                .andReturn().getResponse().getStatus();
    }

    /** Codigo de uma habilidade liberada que nao e da classe (nem GERAL) do personagem. */
    private String deOutraClasse(long id, String classe, int pular) throws Exception {
        int n = 0;
        for (JsonNode h : ler("/api/personagens/" + id + "/habilidades/disponiveis")) {
            String dono = h.get("classeCodigo").asText();
            if (!h.get("bloqueada").asBoolean() && !dono.contains(classe) && !dono.contains("GERAL") && !dono.isBlank()
                    && n++ == pular) {
                return h.get("codigo").asText();
            }
        }
        throw new AssertionError("nenhuma habilidade liberada de outra classe");
    }

    @Test
    void criacaoNaoEscolheHabilidadePeloJogador() throws Exception {
        long org = org();
        for (String classe : new String[]{"MAGO", "LADRAO", "CAVALEIRO"}) {
            long id = criar(org, "ELFO", classe, 1);
            assertEquals(0, ler("/api/personagens/" + id + "/habilidades").size(), classe + ": ficha nasce sem habilidade");
            assertTrue(ler("/api/personagens/" + id + "/habilidades/disponiveis").size() > 0, classe + ": o jogador escolhe");
        }
    }

    @Test
    void versatilidadeHumanaPermiteUmaHabilidadeDeOutraClasse() throws Exception {
        long org = org();
        long humano = criar(org, "HUMANO", "MAGO", 20);
        String fora = deOutraClasse(humano, "MAGO", 0);
        String outraFora = deOutraClasse(humano, "MAGO", 1);
        assertEquals(201, adicionar(humano, fora));
        assertEquals(400, adicionar(humano, outraFora), "so 1 de fora da classe");
        // a limpeza ao abrir a ficha nao apaga a escolhida
        assertEquals(1, ler("/api/personagens/" + humano + "/habilidades").size());

        long elfo = criar(org, "ELFO", "MAGO", 20);
        assertEquals(400, adicionar(elfo, fora), "so Humano tem Versatilidade");
        long humano19 = criar(org, "HUMANO", "MAGO", 19);
        assertEquals(400, adicionar(humano19, fora), "antes do nivel 20 nao vale");
    }
}
