package com.asus.platform.config;

import com.asus.platform.security.JwtAuthFilter;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Arrays;
import java.util.List;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * Seguranca stateless com JWT (Fase 7).
 *
 * <p>Por padrao ({@code asus.security.enforce=false}) a API fica aberta para
 * desenvolvimento — apenas {@code /api/auth/me} exige token, provando o fluxo.
 * Em producao defina {@code asus.security.enforce=true} para exigir autenticacao
 * em todo {@code /api/**} (exceto rotas publicas abaixo).</p>
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;
    private final ObjectProvider<OAuth2SuccessHandler> oauthSuccessHandler;

    @Value("${asus.security.enforce:false}")
    private boolean enforce;

    @Value("${asus.cors.allowed-origins:*}")
    private String allowedOrigins;

    @Value("${asus.oauth.google.enabled:false}")
    private boolean googleEnabled;

    public SecurityConfig(JwtAuthFilter jwtAuthFilter,
                          ObjectProvider<OAuth2SuccessHandler> oauthSuccessHandler) {
        this.jwtAuthFilter = jwtAuthFilter;
        this.oauthSuccessHandler = oauthSuccessHandler;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .headers(h -> h.frameOptions(f -> f.disable())) // H2 console
                .authorizeHttpRequests(reg -> {
                    reg.requestMatchers("/api/auth/login", "/api/auth/register",
                            "/api/auth/refresh", "/api/auth/config",
                            "/api/auth/verificar-email", "/api/auth/esqueci-senha",
                            "/api/auth/redefinir-senha").permitAll();
                    reg.requestMatchers("/api/auth/me").authenticated();
                    reg.requestMatchers("/api/legal/**", "/api/sistemas/**").permitAll();
                    // Webhooks (ex.: Stripe) e o fluxo OAuth2 do Google sao publicos.
                    reg.requestMatchers("/api/webhooks/**", "/oauth2/**", "/login/oauth2/**").permitAll();
                    reg.requestMatchers("/ws/**", "/ws-sockjs/**", "/h2-console/**", "/error").permitAll();
                    // O conteudo de um asset (avatar/capa) e carregado via <img> e background-image,
                    // que NAO enviam o header Authorization. Liberamos o GET por id para as imagens
                    // aparecerem mesmo com a seguranca ligada (o upload/listagem seguem protegidos).
                    reg.requestMatchers(HttpMethod.GET, "/api/assets/*/conteudo").permitAll();
                    // Info minima do overlay OBS por personagem (nome + retrato): pagina publica sem login.
                    reg.requestMatchers(HttpMethod.GET, "/api/personagens/*/overlay").permitAll();
                    // Overlay da MESA inteira (retratos + barras + iniciativa) — pagina publica pro OBS.
                    reg.requestMatchers(HttpMethod.GET, "/api/campanhas/*/overlay").permitAll();
                    // Ficha compartilhada por link publico (read-only, token opaco).
                    reg.requestMatchers(HttpMethod.GET, "/api/publico/**").permitAll();
                    if (enforce) {
                        reg.requestMatchers("/api/**").authenticated();
                    }
                    reg.anyRequest().permitAll();
                })
                .httpBasic(b -> b.disable())
                .formLogin(f -> f.disable())
                .exceptionHandling(e -> e.authenticationEntryPoint(
                        (req, res, ex) -> res.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Nao autenticado")))
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        // "Entrar com Google": só ativa quando asus.oauth.google.enabled=true
        // (e o ClientRegistrationRepository do GoogleOAuthConfig existe).
        if (googleEnabled) {
            http.oauth2Login(o -> o.successHandler(oauthSuccessHandler.getObject()));
        }
        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration cfg = new CorsConfiguration();
        if ("*".equals(allowedOrigins.trim())) {
            cfg.addAllowedOriginPattern("*"); // permite curinga com credenciais
        } else {
            cfg.setAllowedOrigins(Arrays.stream(allowedOrigins.split(",")).map(String::trim).toList());
        }
        cfg.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        cfg.addAllowedHeader("*");
        cfg.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", cfg);
        return source;
    }
}
