package com.asus.platform;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/** Escudo do Mestre: visao consolidada, edicao de status e rolagens ocultas (Fase 8). */
@SpringBootTest
@AutoConfigureMockMvc
class EscudoIntegrationTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @Test
    void painelDoMestre() throws Exception {
        long orgId = readId(mockMvc.perform(post("/api/organizacoes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Org Escudo\",\"slug\":\"escudo-test\"}"))
                .andExpect(status().isCreated()).andReturn());

        long campId = readId(mockMvc.perform(post("/api/organizacoes/" + orgId + "/campanhas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Camp Escudo\",\"mestreId\":1}"))
                .andExpect(status().isCreated()).andReturn());

        long pId = readId(mockMvc.perform(post("/api/organizacoes/" + orgId + "/personagens")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Varg\",\"racaCodigo\":\"ANAO\",\"classeCodigo\":\"GUERREIRO\","
                                + "\"nivel\":1,\"atributosBase\":{\"forca\":4,\"agilidade\":2,\"vigor\":4,"
                                + "\"intelecto\":1,\"presenca\":1}}"))
                .andExpect(status().isCreated()).andReturn());

        mockMvc.perform(post("/api/campanhas/" + campId + "/personagens")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"personagemId\":" + pId + "}"))
                .andExpect(status().isCreated());

        // Rolagem oculta na campanha
        mockMvc.perform(post("/api/campanhas/" + campId + "/rolagens")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"expressao\":\"1d20\",\"oculta\":true,\"usuarioId\":1}"))
                .andExpect(status().isCreated());

        // Painel consolidado (mestre = 1)
        mockMvc.perform(get("/api/campanhas/" + campId + "/escudo?usuarioId=1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.campanha.id").value((int) campId))
                .andExpect(jsonPath("$.personagens[0].nome").value("Varg"))
                .andExpect(jsonPath("$.rolagens[0].oculta").value(true))
                // mestre ve o resultado da oculta (nao mascarado)
                .andExpect(jsonPath("$.rolagens[0].total").isNotEmpty());

        // Mestre edita o PV do personagem
        mockMvc.perform(patch("/api/campanhas/" + campId + "/escudo/personagens/" + pId + "/status?usuarioId=1")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"pvAtual\":7}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status.pvAtual").value(7));

        // Sem ser membro -> 400
        mockMvc.perform(get("/api/campanhas/" + campId + "/escudo?usuarioId=999"))
                .andExpect(status().isBadRequest());
    }

    private long readId(MvcResult result) throws Exception {
        JsonNode node = objectMapper.readTree(result.getResponse().getContentAsString());
        return node.get("id").asLong();
    }
}
