package com.asus.platform;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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

/** Round-trip: exportar e reimportar usando o mesmo envelope (Fase 3). */
@SpringBootTest
@AutoConfigureMockMvc
class ImportPersonagemIntegrationTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @Test
    void exportarEReimportar() throws Exception {
        long orgId = readId(mockMvc.perform(post("/api/organizacoes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Org Import\",\"slug\":\"import-test\"}"))
                .andExpect(status().isCreated())
                .andReturn());

        String personagemJson = """
            {"nome":"Original","jogador":"Cau","racaCodigo":"ANAO","classeCodigo":"GUERREIRO",
             "nivel":2,"atributosBase":{"forca":4,"agilidade":2,"vigor":3,"intelecto":1,"presenca":1}}
            """;
        MvcResult criado = mockMvc.perform(post("/api/organizacoes/" + orgId + "/personagens")
                        .contentType(MediaType.APPLICATION_JSON).content(personagemJson))
                .andExpect(status().isCreated())
                .andReturn();
        long originalId = readId(criado);
        int pvMaxOriginal = objectMapper.readTree(criado.getResponse().getContentAsString())
                .get("status").get("pvMax").asInt();

        // Exporta o envelope completo
        String envelope = mockMvc.perform(get("/api/personagens/" + originalId + "/export"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        // Reimporta o MESMO envelope (campos extras do export sao ignorados)
        mockMvc.perform(post("/api/personagens/import")
                        .contentType(MediaType.APPLICATION_JSON).content(envelope))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nome").value("Original"))
                .andExpect(jsonPath("$.organizacaoId").value((int) orgId))
                .andExpect(jsonPath("$.nivel").value(2))
                .andExpect(jsonPath("$.status.pvMax").value(pvMaxOriginal));

        // Agora a organizacao tem 2 personagens
        mockMvc.perform(get("/api/organizacoes/" + orgId + "/personagens"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    private long readId(MvcResult result) throws Exception {
        JsonNode node = objectMapper.readTree(result.getResponse().getContentAsString());
        return node.get("id").asLong();
    }
}
