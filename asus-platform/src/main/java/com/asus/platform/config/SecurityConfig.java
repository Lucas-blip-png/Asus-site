package com.asus.platform.config;

import com.asus.platform.security.JwtAuthFilter;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Arrays;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
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

    @Value("${asus.security.enforce:false}")
    private boolean enforce;

    @Value("${asus.cors.allowed-origins:*}")
    private String allowedOrigins;

    public SecurityConfig(JwtAuthFilter jwtAuthFilter) {
        this.jwtAuthFilter = jwtAuthFilter;
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
                            "/api/auth/refresh").permitAll();
                    reg.requestMatchers("/api/auth/me").authenticated();
                    reg.requestMatchers("/api/legal/**", "/api/sistemas/**").permitAll();
                    reg.requestMatchers("/ws/**", "/ws-sockjs/**", "/h2-console/**", "/error").permitAll();
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
