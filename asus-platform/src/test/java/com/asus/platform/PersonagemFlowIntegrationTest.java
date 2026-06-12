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
        // Humano (PV/PM/PE 5/5/5) + Cavaleiro (+2 For, +2 Con, +1 Car)
        String personagemJson = """
            {
              "nome":"Thorin",
              "jogador":"Ana",
              "racaCodigo":"HUMANO",
              "classeCodigo":"CAVALEIRO",
              "nivel":1,
              "atributosBase":{"forca":0,"constituicao":2,"destreza":2,"agilidade":1,"inteligencia":0,"sabedoria":0,"carisma":0}
            }
            """;
        MvcResult pResult = mockMvc.perform(
                        post("/api/organizacoes/" + orgId + "/personagens")
                                .contentType(MediaType.APPLICATION_JSON).content(personagemJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nome").value("Thorin"))
                .andExpect(jsonPath("$.rulesetVersion").value("ASUS_V1"))
                // Con final 4 (2 base + 2 classe), Cavaleiro PV/PM/PE 6/3/6 -> PV = 5 + 6 + 4*2 = 19
                .andExpect(jsonPath("$.status.pvMax").value(19))
                // PM = 5 + 3 + Int(0)*2 = 8
                .andExpect(jsonPath("$.status.pmMax").value(8))
                // PE = 5 + 6 + Con(4)*2 = 19
                .andExpect(jsonPath("$.status.peMax").value(19))
                .andExpect(jsonPath("$.deslocamento").value(4))
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
                .andExpect(jsonPath("$.atributosFinais.constituicao").value(4));

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
                .andExpect(jsonPath("$.status.pvMax").value(19));
    }

    private long readId(MvcResult result) throws Exception {
        JsonNode node = objectMapper.readTree(result.getResponse().getContentAsString());
        return node.get("id").asLong();
    }
}
