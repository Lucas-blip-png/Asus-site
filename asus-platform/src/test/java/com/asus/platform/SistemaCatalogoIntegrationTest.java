package com.asus.platform;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

/** Catalogos do sistema ASUS real: atributos, racas, classes, pericias, habilidades, itens, progressao. */
@SpringBootTest
@AutoConfigureMockMvc
class SistemaCatalogoIntegrationTest {

    @Autowired
    MockMvc mockMvc;

    @Test
    void exponeOConteudoRealDoSistema() throws Exception {
        // 7 atributos
        mockMvc.perform(get("/api/sistemas/asus/atributos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(7))
                .andExpect(jsonPath("$[0].sigla").value("For"));

        // 13 racas, com PV/PM/PE base
        mockMvc.perform(get("/api/sistemas/asus/racas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(13));

        // 16 classes-base
        mockMvc.perform(get("/api/sistemas/asus/classes?base=true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(16));

        // 26 pericias
        mockMvc.perform(get("/api/sistemas/asus/pericias"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(26));

        // Habilidades e itens (catalogo representativo)
        mockMvc.perform(get("/api/sistemas/asus/habilidades"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(Matchers.greaterThanOrEqualTo(10)));
        mockMvc.perform(get("/api/sistemas/asus/itens"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(Matchers.greaterThanOrEqualTo(10)))
                .andExpect(jsonPath("$[0].moeda").value("T$"));

        // 50 niveis de progressao
        mockMvc.perform(get("/api/sistemas/asus/progressao"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(50))
                .andExpect(jsonPath("$[0].nivel").value(1))
                .andExpect(jsonPath("$[0].xpNecessario").value(0))
                .andExpect(jsonPath("$[49].limiteAtributo").value(40));

        // Regras de feitico
        mockMvc.perform(get("/api/sistemas/asus/feiticos/regras"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.circulos.length()").value(4));
    }
}
