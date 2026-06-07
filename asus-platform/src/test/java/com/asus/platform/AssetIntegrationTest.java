package com.asus.platform;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/** Upload, listagem, download e exclusao de assets (Fase 11). */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = "asus.uploads-dir=target/test-uploads")
class AssetIntegrationTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @Test
    void cicloDeVidaDoAsset() throws Exception {
        long orgId = readId(mockMvc.perform(post("/api/organizacoes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Org Asset\",\"slug\":\"asset-test\"}"))
                .andExpect(status().isCreated())
                .andReturn());

        byte[] dados = "conteudo-de-teste-do-asset".getBytes();
        MockMultipartFile arquivo =
                new MockMultipartFile("file", "avatar.png", "image/png", dados);

        MvcResult upload = mockMvc.perform(multipart("/api/organizacoes/" + orgId + "/assets")
                        .file(arquivo)
                        .param("tipo", "AVATAR_PERSONAGEM"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tipo").value("AVATAR_PERSONAGEM"))
                .andExpect(jsonPath("$.nomeOriginal").value("avatar.png"))
                .andExpect(jsonPath("$.tamanhoBytes").value(dados.length))
                .andExpect(jsonPath("$.url").exists())
                .andReturn();
        long assetId = readId(upload);

        mockMvc.perform(get("/api/organizacoes/" + orgId + "/assets"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value((int) assetId));

        mockMvc.perform(get("/api/assets/" + assetId + "/conteudo"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.IMAGE_PNG))
                .andExpect(content().bytes(dados));

        mockMvc.perform(delete("/api/assets/" + assetId))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/assets/" + assetId + "/conteudo"))
                .andExpect(status().isNotFound());
    }

    private long readId(MvcResult result) throws Exception {
        JsonNode node = objectMapper.readTree(result.getResponse().getContentAsString());
        return node.get("id").asLong();
    }
}
