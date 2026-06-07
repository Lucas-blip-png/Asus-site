package com.asus.platform;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.asus.platform.domain.Usuario;
import com.asus.platform.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/** Exportar dados, consentimento e exclusao/anonimizacao de conta (Fase 13). */
@SpringBootTest
@AutoConfigureMockMvc
class LgpdIntegrationTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    UsuarioRepository usuarioRepository;

    @Test
    void exportarConsentirEExcluir() throws Exception {
        // Usuario descartavel (nao mexe no dev compartilhado)
        Usuario u = usuarioRepository.save(Usuario.builder()
                .nome("Titular LGPD").email("lgpd@test.local").build());
        long id = u.getId();

        // Registrar consentimento
        mockMvc.perform(post("/api/me/consentimentos?usuarioId=" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tipo\":\"TERMOS\",\"versaoDocumento\":\"1.0\",\"aceito\":true}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tipo").value("TERMOS"))
                .andExpect(jsonPath("$.aceito").value(true));

        // Exportar dados
        mockMvc.perform(get("/api/me/export-data?usuarioId=" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.usuario.id").value((int) id))
                .andExpect(jsonPath("$.consentimentos.length()").value(1));

        // Excluir/anonimizar conta
        mockMvc.perform(delete("/api/me/delete-account?usuarioId=" + id))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/me/export-data?usuarioId=" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.usuario.anonimizado").value(true))
                .andExpect(jsonPath("$.usuario.nome").value("Usuario removido"));

        // Documentos legais
        mockMvc.perform(get("/api/legal/termos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.documento").value("TERMOS"))
                .andExpect(jsonPath("$.conteudo").exists());
        mockMvc.perform(get("/api/legal/privacidade"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.documento").value("PRIVACIDADE"));
    }
}
