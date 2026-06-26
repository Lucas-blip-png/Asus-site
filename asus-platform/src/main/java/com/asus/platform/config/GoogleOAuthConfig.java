package com.asus.platform.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.oauth2.client.CommonOAuth2Provider;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;

/**
 * Provedor Google para "Entrar com Google" (OAuth2).
 *
 * <p>Construído programaticamente a partir de {@code asus.oauth.google.client-id/secret}
 * e só registrado quando {@code asus.oauth.google.enabled=true} — assim a dependência
 * {@code oauth2-client} fica inerte por padrão e não exige credenciais nos testes.</p>
 */
@Configuration
@ConditionalOnProperty(name = "asus.oauth.google.enabled", havingValue = "true")
public class GoogleOAuthConfig {

    @Bean
    public ClientRegistrationRepository clientRegistrationRepository(
            @Value("${asus.oauth.google.client-id}") String clientId,
            @Value("${asus.oauth.google.client-secret}") String clientSecret) {
        ClientRegistration google = CommonOAuth2Provider.GOOGLE
                .getBuilder("google")
                .clientId(clientId)
                .clientSecret(clientSecret)
                .build();
        return new InMemoryClientRegistrationRepository(google);
    }
}
