package com.asus.platform;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/** Seções 16 (Analytics), 17 (Notificações), 18 (Sessões), 21.1 (Membros) + conteúdo da ficha e bestiário. */
@SpringBootTest
@AutoConfigureMockMvc
class NovasSecoesIntegrationTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @Test
    void fechaTodasAsSecoes() throws Exception {
        long orgId = readId("/api/organizacoes", "{\"nome\":\"Org Secoes\",\"slug\":\"secoes-test\"}");

        // 21.1 — membros da organizacao + PUT
        mockMvc.perform(put("/api/organizacoes/" + orgId)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"nome\":\"Renomeada\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.nome").value("Renomeada"));
        mockMvc.perform(post("/api/organizacoes/" + orgId + "/membros")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"usuarioId\":1,\"papel\":\"ADMIN\"}"))
                .andExpect(status().isCreated());
        mockMvc.perform(get("/api/organizacoes/" + orgId + "/membros"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1));
        mockMvc.perform(delete("/api/organizacoes/" + orgId + "/membros/1")).andExpect(status().isNoContent());

        // 16 — Analytics
        mockMvc.perform(post("/api/analytics")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"evento\":\"PERSONAGEM_CRIADO\",\"organizacaoId\":" + orgId + "}"))
                .andExpect(status().isCreated());
        mockMvc.perform(get("/api/organizacoes/" + orgId + "/analytics"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1));

        // 17 — Notificações
        long notifId = readId("/api/notificacoes",
                "{\"usuarioId\":1,\"titulo\":\"Convite\",\"mensagem\":\"Bem-vindo\"}");
        mockMvc.perform(get("/api/me/notificacoes?usuarioId=1"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(Matchers.greaterThanOrEqualTo(1)));
        mockMvc.perform(post("/api/notificacoes/" + notifId + "/lida"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.lida").value(true));

        // 18 — Sessões e presença (campanha tambem cobre a sanitização da Seção 20.1: FREE so permite 1 campanha)
        long campId = readId("/api/organizacoes/" + orgId + "/campanhas",
                "{\"nome\":\"Camp Secoes\",\"descricao\":\"<script>alert(1)</script>Olá\"}");
        mockMvc.perform(get("/api/campanhas/" + campId))
                .andExpect(status().isOk()).andExpect(jsonPath("$.descricao").value("alert(1)Olá"));
        long sessaoId = readId("/api/campanhas/" + campId + "/sessoes",
                "{\"titulo\":\"Sessão 1\",\"descricao\":\"Abertura\"}");
        mockMvc.perform(get("/api/campanhas/" + campId + "/sessoes"))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].titulo").value("Sessão 1"));
        mockMvc.perform(post("/api/sessoes/" + sessaoId + "/presenca")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"usuarioId\":1,\"status\":\"CONFIRMADO\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("CONFIRMADO"));

        // Conteúdo da ficha: ataques e feitiços
        long pId = readId("/api/organizacoes/" + orgId + "/personagens",
                "{\"nome\":\"Heitor\",\"racaCodigo\":\"HUMANO\",\"classeCodigo\":\"CAVALEIRO\",\"nivel\":1,"
                + "\"atributosBase\":{\"forca\":0,\"constituicao\":2,\"destreza\":2,\"agilidade\":1,"
                + "\"inteligencia\":0,\"sabedoria\":0,\"carisma\":0}}");
        mockMvc.perform(post("/api/personagens/" + pId + "/ataques")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Espadada\",\"dano\":\"1d8\",\"critico\":\"x2\"}"))
                .andExpect(status().isCreated());
        mockMvc.perform(get("/api/personagens/" + pId + "/ataques"))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].nome").value("Espadada"));
        mockMvc.perform(post("/api/personagens/" + pId + "/feiticos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Bola de Fogo\",\"circulo\":2,\"custoPm\":4}"))
                .andExpect(status().isCreated());
        mockMvc.perform(get("/api/personagens/" + pId + "/feiticos"))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].nome").value("Bola de Fogo"));

        // Regra dos 5 pontos na criacao -> 400
        mockMvc.perform(post("/api/organizacoes/" + orgId + "/personagens")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Excesso\",\"racaCodigo\":\"HUMANO\",\"classeCodigo\":\"CAVALEIRO\","
                                + "\"atributosBase\":{\"forca\":4,\"constituicao\":4,\"destreza\":0,\"agilidade\":0,"
                                + "\"inteligencia\":0,\"sabedoria\":0,\"carisma\":0}}"))
                .andExpect(status().isBadRequest());

        // Perícia "Outros" (concedida por item) aparece na ficha como custom
        mockMvc.perform(put("/api/personagens/" + pId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"periciasCustom\":[{\"nome\":\"Pilotagem\",\"atributo\":\"DESTREZA\",\"treino\":1}]}"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/personagens/" + pId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pericias[?(@.nome == 'Pilotagem')]").isNotEmpty());

        // Bestiário
        mockMvc.perform(post("/api/bestiario")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Goblin Batedor\",\"nivel\":1,\"especie\":\"Goblin\",\"pv\":12}"))
                .andExpect(status().isCreated());
        mockMvc.perform(get("/api/bestiario"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(Matchers.greaterThanOrEqualTo(1)));
    }

    private long readId(String url, String body) throws Exception {
        MvcResult r = mockMvc.perform(post(url).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated()).andReturn();
        return objectMapper.readTree(r.getResponse().getContentAsString()).get("id").asLong();
    }
}
