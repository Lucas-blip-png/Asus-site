package com.asus.platform;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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

/** Limites por plano e upgrade manual de assinatura (Fase 10). */
@SpringBootTest
@AutoConfigureMockMvc
class PlanoLimiteIntegrationTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @Test
    void limiteDeCampanhasRespeitaPlanoEUpgrade() throws Exception {
        long orgId = readId(mockMvc.perform(post("/api/organizacoes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Org Plano\",\"slug\":\"plano-test\"}"))
                .andExpect(status().isCreated())
                .andReturn());

        // FREE permite 1 campanha
        mockMvc.perform(get("/api/organizacoes/" + orgId + "/limites"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.maxCampanhas").value(1));

        mockMvc.perform(post("/api/organizacoes/" + orgId + "/campanhas")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"nome\":\"C1\"}"))
                .andExpect(status().isCreated());

        // 2a campanha estoura o limite do FREE -> 403
        mockMvc.perform(post("/api/organizacoes/" + orgId + "/campanhas")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"nome\":\"C2\"}"))
                .andExpect(status().isForbidden());

        // Upgrade manual para PRO
        mockMvc.perform(put("/api/organizacoes/" + orgId + "/assinatura")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"plano\":\"PRO\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.plano").value("PRO"))
                .andExpect(jsonPath("$.status").value("ATIVA"));

        // Agora a 2a campanha passa (PRO permite 3)
        mockMvc.perform(post("/api/organizacoes/" + orgId + "/campanhas")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"nome\":\"C2\"}"))
                .andExpect(status().isCreated());
    }

    private long readId(MvcResult result) throws Exception {
        JsonNode node = objectMapper.readTree(result.getResponse().getContentAsString());
        return node.get("id").asLong();
    }
}
