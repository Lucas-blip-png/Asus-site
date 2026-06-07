package com.asus.platform;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.asus.platform.engine.Dado;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayDeque;
import java.util.Deque;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@Import(RolagemFlowIntegrationTest.DadoStubConfig.class)
class RolagemFlowIntegrationTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    Dado dado; // resolve para FilaDado (via @Primary)

    @TestConfiguration
    static class DadoStubConfig {
        @Bean
        @Primary
        Dado dadoStub() {
            return new FilaDado();
        }
    }

    /** Dado controlavel: devolve as faces programadas, ou 1 se a fila esvaziar. */
    static class FilaDado implements Dado {
        final Deque<Integer> proximos = new ArrayDeque<>();

        void programar(int... faces) {
            proximos.clear();
            for (int f : faces) {
                proximos.add(f);
            }
        }

        @Override
        public int rolar(int faces) {
            Integer v = proximos.poll();
            return v == null ? 1 : v;
        }
    }

    private FilaDado fila() {
        return (FilaDado) dado;
    }

    @Test
    void rolagemNormalCriticoEFalha() throws Exception {
        long campId = criarCampanha("rol-normal");

        // Rolagem normal: 1d20+5 com d20 = 14 -> total 19, sem critico/falha
        fila().programar(14);
        mockMvc.perform(rolar(campId, "{\"expressao\":\"1d20+5\",\"rotulo\":\"Ataque\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.total").value(19))
                .andExpect(jsonPath("$.naturalD20").value(14))
                .andExpect(jsonPath("$.critico").value(false))
                .andExpect(jsonPath("$.falhaCritica").value(false))
                .andExpect(jsonPath("$.detalhe").value("[14]+5"));

        // Critico: d20 natural = 20
        fila().programar(20);
        mockMvc.perform(rolar(campId, "{\"expressao\":\"1d20\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.total").value(20))
                .andExpect(jsonPath("$.critico").value(true))
                .andExpect(jsonPath("$.falhaCritica").value(false));

        // Falha critica: d20 natural = 1
        fila().programar(1);
        mockMvc.perform(rolar(campId, "{\"expressao\":\"1d20\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.critico").value(false))
                .andExpect(jsonPath("$.falhaCritica").value(true));

        // Historico: mais recente primeiro (a falha critica)
        mockMvc.perform(get("/api/campanhas/" + campId + "/rolagens"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].falhaCritica").value(true))
                .andExpect(jsonPath("$[2].rotulo").value("Ataque"));
    }

    @Test
    void rolagemOcultaMascaraAteRevelar() throws Exception {
        long campId = criarCampanha("rol-oculta");

        // Rolagem oculta: quem rola ve o resultado
        fila().programar(15);
        MvcResult res = mockMvc.perform(rolar(campId,
                        "{\"expressao\":\"1d20\",\"oculta\":true}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.total").value(15))
                .andExpect(jsonPath("$.oculta").value(true))
                .andExpect(jsonPath("$.revelada").value(false))
                .andReturn();
        long rolId = readId(res);

        // No historico, vem mascarada
        mockMvc.perform(get("/api/campanhas/" + campId + "/rolagens"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].oculta").value(true))
                .andExpect(jsonPath("$[0].total").isEmpty())
                .andExpect(jsonPath("$[0].detalhe").isEmpty());

        // Mestre (usuario 1) revela
        mockMvc.perform(post("/api/campanhas/" + campId + "/rolagens/" + rolId + "/revelar")
                        .param("usuarioId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.revelada").value(true))
                .andExpect(jsonPath("$.total").value(15));

        // Agora o historico mostra o resultado
        mockMvc.perform(get("/api/campanhas/" + campId + "/rolagens"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].total").value(15));
    }

    @Test
    void revelarSemSerMembroFalha() throws Exception {
        long campId = criarCampanha("rol-perm");

        fila().programar(10);
        long rolId = readId(mockMvc.perform(rolar(campId,
                        "{\"expressao\":\"1d20\",\"oculta\":true}"))
                .andExpect(status().isCreated())
                .andReturn());

        // Usuario 999 nao e membro -> sem permissao -> 400
        mockMvc.perform(post("/api/campanhas/" + campId + "/rolagens/" + rolId + "/revelar")
                        .param("usuarioId", "999"))
                .andExpect(status().isBadRequest());
    }

    // ----- helpers -----

    private long criarCampanha(String slug) throws Exception {
        long orgId = readId(mockMvc.perform(post("/api/organizacoes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Org " + slug + "\",\"slug\":\"" + slug + "\"}"))
                .andExpect(status().isCreated())
                .andReturn());

        // mestreId = 1 (usuario dev semeado) -> vira membro MESTRE da campanha
        return readId(mockMvc.perform(post("/api/organizacoes/" + orgId + "/campanhas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Camp " + slug + "\",\"mestreId\":1}"))
                .andExpect(status().isCreated())
                .andReturn());
    }

    private static org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder rolar(
            long campId, String body) {
        return post("/api/campanhas/" + campId + "/rolagens")
                .contentType(MediaType.APPLICATION_JSON).content(body);
    }

    private long readId(MvcResult result) throws Exception {
        JsonNode node = objectMapper.readTree(result.getResponse().getContentAsString());
        return node.get("id").asLong();
    }
}
