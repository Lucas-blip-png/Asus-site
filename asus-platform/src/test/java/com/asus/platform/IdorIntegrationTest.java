package com.asus.platform;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/** Usuario B (sem relacao com A) nao pode agir sobre os recursos de A. */
@SpringBootTest
@AutoConfigureMockMvc
class IdorIntegrationTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    private JsonNode json(String body) throws Exception {
        return objectMapper.readTree(body);
    }

    private JsonNode registrar(String email) throws Exception {
        return json(mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"U\",\"email\":\"" + email + "\",\"senha\":\"segredo123\"}"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString());
    }

    private static MockHttpServletRequestBuilder como(MockHttpServletRequestBuilder req, JsonNode u, String corpo) {
        req.header(HttpHeaders.AUTHORIZATION, "Bearer " + u.get("accessToken").asText());
        return corpo == null ? req : req.contentType(MediaType.APPLICATION_JSON).content(corpo);
    }

    private long idDe(MockHttpServletRequestBuilder req) throws Exception {
        return json(mockMvc.perform(req).andExpect(status().is2xxSuccessful()).andReturn()
                .getResponse().getContentAsString()).get("id").asLong();
    }

    @Test
    void usuarioEstranhoNaoAcessaRecursosDeOutro() throws Exception {
        JsonNode a = registrar("idor-a@test.local");
        JsonNode b = registrar("idor-b@test.local");
        long bId = b.get("usuario").get("id").asLong();

        // Organizacao: B nao lista nem adiciona membros; A (dono) pode.
        long org = idDe(como(post("/api/organizacoes"), a, "{\"nome\":\"Mesa A\",\"slug\":\"idor-mesa-a\"}"));
        mockMvc.perform(como(get("/api/organizacoes/" + org + "/membros"), b, null))
                .andExpect(status().isForbidden());
        mockMvc.perform(como(post("/api/organizacoes/" + org + "/membros"), b, "{\"usuarioId\":" + bId + "}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(como(post("/api/organizacoes/" + org + "/membros"), a, "{\"usuarioId\":" + bId + "}"))
                .andExpect(status().isCreated());
        // B virou JOGADOR: ve a lista, mas continua sem poder gerir membros.
        mockMvc.perform(como(get("/api/organizacoes/" + org + "/membros"), b, null)).andExpect(status().isOk());
        mockMvc.perform(como(post("/api/organizacoes/" + org + "/membros"), b, "{\"usuarioId\":" + bId + "}"))
                .andExpect(status().isForbidden());

        // Campanha de A: B (jogador da org, mas nao da campanha) nao cria convite nem mexe em combate.
        long camp = idDe(como(post("/api/organizacoes/" + org + "/campanhas"), a, "{\"nome\":\"Camp A\"}"));
        long combate = idDe(como(post("/api/campanhas/" + camp + "/combates"), a, "{\"nome\":\"C1\"}"));
        mockMvc.perform(como(post("/api/campanhas/" + camp + "/convites"), b, "{\"papel\":\"JOGADOR\"}"))
                .andExpect(status().is4xxClientError());
        mockMvc.perform(como(post("/api/campanhas/" + camp + "/combates"), b, "{\"nome\":\"X\"}"))
                .andExpect(status().is4xxClientError());
        mockMvc.perform(como(delete("/api/combates/" + combate), b, null)).andExpect(status().is4xxClientError());
        mockMvc.perform(como(get("/api/combates/" + combate + "/participantes"), b, null))
                .andExpect(status().is4xxClientError());
        mockMvc.perform(como(post("/api/campanhas/" + camp + "/convites"), a, "{\"papel\":\"JOGADOR\"}"))
                .andExpect(status().isCreated());

        // Bestiario: so o criador apaga a criatura.
        long criatura = idDe(como(post("/api/bestiario"), a, "{\"nome\":\"Lobo\"}"));
        mockMvc.perform(como(delete("/api/bestiario/" + criatura), b, null)).andExpect(status().isForbidden());
        mockMvc.perform(como(delete("/api/bestiario/" + criatura), a, null)).andExpect(status().isNoContent());

        // LGPD: B passando ?usuarioId=A so atinge a propria conta; A continua logando.
        mockMvc.perform(como(delete("/api/me/delete-account?usuarioId=" + a.get("usuario").get("id").asLong()), b, null))
                .andExpect(status().isNoContent());
        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"idor-a@test.local\",\"senha\":\"segredo123\"}"))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"idor-b@test.local\",\"senha\":\"segredo123\"}"))
                .andExpect(status().isUnauthorized());
    }
}
