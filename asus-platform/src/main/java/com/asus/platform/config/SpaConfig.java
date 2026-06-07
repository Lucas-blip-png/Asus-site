package com.asus.platform.config;

import java.io.IOException;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.lang.NonNull;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.PathResourceResolver;

/**
 * Faz o Spring servir a SPA React (buildada em {@code classpath:/static/}) e
 * redireciona rotas do cliente (ex.: /personagens, /campanhas/1) para o
 * {@code index.html} — sem interferir em {@code /api/**}, {@code /ws} ou no
 * console H2. Em dev (sem frontend embutido) o fallback simplesmente nao encontra
 * o index e devolve 404, entao nada quebra.
 */
@Configuration
public class SpaConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(@NonNull ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/**")
                .addResourceLocations("classpath:/static/")
                .resourceChain(true)
                .addResolver(new PathResourceResolver() {
                    @Override
                    protected Resource getResource(@NonNull String resourcePath, @NonNull Resource location)
                            throws IOException {
                        Resource requested = location.createRelative(resourcePath);
                        if (requested.exists() && requested.isReadable()) {
                            return requested;
                        }
                        // Rotas de API/infra nunca caem na SPA.
                        if (resourcePath.startsWith("api/") || resourcePath.startsWith("ws")
                                || resourcePath.startsWith("h2-console") || resourcePath.equals("error")) {
                            return null;
                        }
                        // Demais rotas (do React Router) recebem o index.html.
                        Resource index = new ClassPathResource("/static/index.html");
                        return index.exists() ? index : null;
                    }
                });
    }
}
