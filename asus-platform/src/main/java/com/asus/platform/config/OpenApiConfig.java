package com.asus.platform.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Documentacao da API em /swagger-ui.html, com o JWT do login como Bearer. */
@Configuration
public class OpenApiConfig {

    @Bean
    OpenAPI asusOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("ASUS RPG Platform API")
                        .version("v1")
                        .description("API REST da plataforma de RPG de mesa: fichas, campanhas, rolagens e mais."))
                .components(new Components().addSecuritySchemes("bearer",
                        new SecurityScheme().type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT")))
                .addSecurityItem(new SecurityRequirement().addList("bearer"));
    }
}
