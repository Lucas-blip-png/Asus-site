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

@SpringBootTest
@AutoConfigureMockMvc
class PersonagemFlowIntegrationTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @Test
    void fluxoCompletoDoMvp() throws Exception {
        // 1) Criar organizacao
        String orgJson = """
            {"nome":"Mesa do Teste","slug":"mesa-teste"}
            """;
        MvcResult orgResult = mockMvc.perform(post("/api/organizacoes")
                        .contentType(MediaType.APPLICATION_JSON).content(orgJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andReturn();
        long orgId = readId(orgResult);

        // 2) Criar personagem ASUS + 3) calculo automatico
        String personagemJson = """
            {
              "nome":"Thorin",
              "jogador":"Ana",
              "racaCodigo":"HUMANO",
              "classeCodigo":"GUERREIRO",
              "nivel":1,
              "atributosBase":{"forca":5,"agilidade":3,"vigor":4,"intelecto":2,"presenca":2}
            }
            """;
        MvcResult pResult = mockMvc.perform(
                        post("/api/organizacoes/" + orgId + "/personagens")
                                .contentType(MediaType.APPLICATION_JSON).content(personagemJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nome").value("Thorin"))
                .andExpect(jsonPath("$.rulesetVersion").value("ASUS_V1"))
                // PV = 12 + 6*1 + vigor(4)*2 = 26
                .andExpect(jsonPath("$.status.pvMax").value(26))
                // PM = 6 + 1*1 + intelectoFinal(3)*2 = 13  (humano da +1 intelecto)
                .andExpect(jsonPath("$.status.pmMax").value(13))
                // PE = 4 + 3*1 + agilidade(3) = 10
                .andExpect(jsonPath("$.status.peMax").value(10))
                // Defesa = 10 + agilidade(3) = 13
                .andExpect(jsonPath("$.status.defesa").value(13))
                .andExpect(jsonPath("$.pericias").isArray())
                .andReturn();
        long personagemId = readId(pResult);

        // 4) Listar personagens da organizacao
        mockMvc.perform(get("/api/organizacoes/" + orgId + "/personagens"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nome").value("Thorin"));

        // 5) Ver ficha completa
        mockMvc.perform(get("/api/personagens/" + personagemId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.atributosFinais.intelecto").value(3));

        // 6) Exportar em JSON
        mockMvc.perform(get("/api/personagens/" + personagemId + "/export"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exportVersion").value("1.0"))
                .andExpect(jsonPath("$.system").value("ASUS"))
                .andExpect(jsonPath("$.personagem.nome").value("Thorin"));

        // 8) Snapshot criado na criacao
        mockMvc.perform(get("/api/personagens/" + personagemId + "/snapshots"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].motivo").value("CRIACAO"));

        // 9) Debug de calculo
        mockMvc.perform(get("/api/personagens/" + personagemId + "/debug"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.passos").isArray())
                .andExpect(jsonPath("$.passos[0]").exists());

        // PATCH status (gera auditoria - criterio 7)
        String patchJson = "{\"pvAtual\":10}";
        mockMvc.perform(patch("/api/personagens/" + personagemId + "/status")
                        .contentType(MediaType.APPLICATION_JSON).content(patchJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status.pvAtual").value(10))
                .andExpect(jsonPath("$.status.pvMax").value(26));
    }

    private long readId(MvcResult result) throws Exception {
        JsonNode node = objectMapper.readTree(result.getResponse().getContentAsString());
        return node.get("id").asLong();
    }
}
