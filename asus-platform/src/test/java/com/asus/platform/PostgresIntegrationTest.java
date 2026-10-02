package com.asus.platform;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Sobe a aplicacao inteira (schema + seed do sistema ASUS) num PostgreSQL real,
 * o mesmo banco de producao, e roda o fluxo principal da ficha.
 */
@Testcontainers
@SpringBootTest(properties = "asus.security.enforce=false")
@ActiveProfiles("postgres")
@AutoConfigureMockMvc
class PostgresIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17-alpine");

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @Test
    void criaOrganizacaoEPersonagemCalculadoNoPostgres() throws Exception {
        String org = mockMvc.perform(post("/api/organizacoes").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Mesa PG\",\"slug\":\"mesa-pg\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long orgId = objectMapper.readTree(org).get("id").asLong();

        String personagem = mockMvc.perform(post("/api/organizacoes/" + orgId + "/personagens")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {"nome":"Thorin","jogador":"Ana","racaCodigo":"HUMANO","classeCodigo":"CAVALEIRO","nivel":1,
                             "atributosBase":{"forca":0,"constituicao":2,"destreza":2,"agilidade":1,"inteligencia":0,"sabedoria":0,"carisma":0}}
                            """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.rulesetVersion").value("ASUS_V1"))
                .andReturn().getResponse().getContentAsString();
        long id = objectMapper.readTree(personagem).get("id").asLong();

        mockMvc.perform(get("/api/personagens/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Thorin"));
    }
}
