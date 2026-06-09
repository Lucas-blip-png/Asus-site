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

@SpringBootTest
@AutoConfigureMockMvc
class CampanhaFlowIntegrationTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @Test
    void fluxoDeCampanha() throws Exception {
        // Organizacao
        long orgId = readId(mockMvc.perform(post("/api/organizacoes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Mesa Campanha\",\"slug\":\"mesa-campanha\"}"))
                .andExpect(status().isCreated())
                .andReturn());

        // Campanha (sem mestre explicito; entra membro depois via convite)
        String campanhaJson = """
            {"nome":"A Queda de Valdoria","descricao":"Aventura inaugural",
             "config":{"usarBencoes":true,"rolagemOcultaPermitida":true}}
            """;
        MvcResult campResult = mockMvc.perform(
                        post("/api/organizacoes/" + orgId + "/campanhas")
                                .contentType(MediaType.APPLICATION_JSON).content(campanhaJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nome").value("A Queda de Valdoria"))
                .andExpect(jsonPath("$.rulesetSystemId").value("ASUS"))
                .andExpect(jsonPath("$.config.usarBencoes").value(true))
                .andReturn();
        long campId = readId(campResult);

        // Personagem na mesma organizacao
        String personagemJson = """
            {"nome":"Lyra","jogador":"Bia","racaCodigo":"ELFO","classeCodigo":"MAGO",
             "nivel":1,"atributosBase":{"forca":0,"constituicao":2,"destreza":1,"agilidade":0,"inteligencia":2,"sabedoria":0,"carisma":0}}
            """;
        long personagemId = readId(mockMvc.perform(
                        post("/api/organizacoes/" + orgId + "/personagens")
                                .contentType(MediaType.APPLICATION_JSON).content(personagemJson))
                .andExpect(status().isCreated())
                .andReturn());

        // Adicionar personagem a campanha
        mockMvc.perform(post("/api/campanhas/" + campId + "/personagens")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"personagemId\":" + personagemId + "}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.personagemId").value((int) personagemId))
                .andExpect(jsonPath("$.personagemNome").value("Lyra"));

        // Personagem repetido -> 400
        mockMvc.perform(post("/api/campanhas/" + campId + "/personagens")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"personagemId\":" + personagemId + "}"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(get("/api/campanhas/" + campId + "/personagens"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].personagemId").value((int) personagemId));

        // Convite
        MvcResult conviteResult = mockMvc.perform(post("/api/campanhas/" + campId + "/convites")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"papel\":\"JOGADOR\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.codigo").exists())
                .andExpect(jsonPath("$.papel").value("JOGADOR"))
                .andReturn();
        String codigo = objectMapper.readTree(conviteResult.getResponse().getContentAsString())
                .get("codigo").asText();

        // Entrar pelo codigo (usuario dev = id 1, semeado no boot)
        mockMvc.perform(post("/api/campanhas/entrar/" + codigo)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"usuarioId\":1}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.usuarioId").value(1))
                .andExpect(jsonPath("$.papel").value("JOGADOR"));

        // Membro aparece na listagem
        mockMvc.perform(get("/api/campanhas/" + campId + "/membros"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].usuarioId").value(1))
                .andExpect(jsonPath("$[0].papel").value("JOGADOR"));

        // Codigo invalido -> 404
        mockMvc.perform(post("/api/campanhas/entrar/NAOEXISTE")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"usuarioId\":1}"))
                .andExpect(status().isNotFound());
    }

    private long readId(MvcResult result) throws Exception {
        JsonNode node = objectMapper.readTree(result.getResponse().getContentAsString());
        return node.get("id").asLong();
    }
}
