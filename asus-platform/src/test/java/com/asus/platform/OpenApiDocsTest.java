package com.asus.platform;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

/** A doc fica publica mesmo com a API exigindo login, e nao cai no fallback da SPA. */
@SpringBootTest(properties = {"asus.security.enforce=true",
        "asus.jwt.secret=segredo-de-teste-com-mais-de-32-caracteres-0123456789"})
@AutoConfigureMockMvc
class OpenApiDocsTest {

    @Autowired MockMvc mockMvc;

    @Test
    void especificacaoOpenApiEhPublica() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.info.title").value("ASUS RPG Platform API"))
                .andExpect(jsonPath("$.components.securitySchemes.bearer.scheme").value("bearer"))
                .andExpect(jsonPath("$.paths['/api/auth/login']").exists());
    }

    @Test
    void swaggerUiEhServido() throws Exception {
        mockMvc.perform(get("/swagger-ui/index.html")).andExpect(status().isOk());
    }
}
