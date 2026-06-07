package com.asus.platform;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
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

/** Marketplace (criar/comprar) e templates (criar/listar/apagar) — Fase 12. */
@SpringBootTest
@AutoConfigureMockMvc
class MarketplaceTemplateIntegrationTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @Test
    void criarEComprarItem() throws Exception {
        // Item pago (autor = dev id 1)
        long itemId = readId(mockMvc.perform(post("/api/marketplace/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"titulo\":\"Aventura: A Cripta\",\"tipo\":\"aventura\","
                                + "\"preco\":25.00,\"autorUsuarioId\":1}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.gratuito").value(false))
                .andExpect(jsonPath("$.publicado").value(true))
                .andReturn());

        mockMvc.perform(get("/api/marketplace"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(org.hamcrest.Matchers.greaterThanOrEqualTo(1)));

        mockMvc.perform(get("/api/marketplace/items/" + itemId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.titulo").value("Aventura: A Cripta"));

        long compraId = readId(mockMvc.perform(post("/api/marketplace/items/" + itemId + "/comprar")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"usuarioId\":1}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.marketplaceItemId").value((int) itemId))
                .andReturn());

        // Idempotente: comprar de novo devolve a mesma compra
        mockMvc.perform(post("/api/marketplace/items/" + itemId + "/comprar")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"usuarioId\":1}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value((int) compraId));

        // Item gratuito -> valorPago 0
        long gratisId = readId(mockMvc.perform(post("/api/marketplace/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"titulo\":\"Token pack gratis\",\"tipo\":\"token\",\"autorUsuarioId\":1}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.gratuito").value(true))
                .andReturn());

        mockMvc.perform(post("/api/marketplace/items/" + gratisId + "/comprar")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"usuarioId\":1}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.valorPago").value(0));
    }

    @Test
    void criarListarApagarTemplate() throws Exception {
        long orgId = readId(mockMvc.perform(post("/api/organizacoes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Org Template\",\"slug\":\"tpl-test\"}"))
                .andExpect(status().isCreated())
                .andReturn());

        long templateId = readId(mockMvc.perform(post("/api/organizacoes/" + orgId + "/templates")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tipo\":\"MAGIA\",\"nome\":\"Bola de Fogo\","
                                + "\"jsonConteudo\":\"{\\\"dano\\\":\\\"3d6\\\"}\",\"publico\":true}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tipo").value("MAGIA"))
                .andReturn());

        mockMvc.perform(get("/api/organizacoes/" + orgId + "/templates"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nome").value("Bola de Fogo"));

        mockMvc.perform(get("/api/templates/publicos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(org.hamcrest.Matchers.greaterThanOrEqualTo(1)));

        mockMvc.perform(delete("/api/templates/" + templateId))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/templates/" + templateId))
                .andExpect(status().isNotFound());
    }

    private long readId(MvcResult result) throws Exception {
        JsonNode node = objectMapper.readTree(result.getResponse().getContentAsString());
        return node.get("id").asLong();
    }
}
