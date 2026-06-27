package com.asus.platform.config;

import com.asus.platform.service.AuthService;
import com.asus.platform.web.dto.AuthResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

/**
 * Após o login Google, encontra/cria o usuário pelo e-mail, emite o MESMO par de
 * tokens JWT do login normal e redireciona o frontend com os tokens no fragmento
 * (#access_token=...&refresh_token=...). Assim o restante do app continua usando
 * JWT, sem saber que a origem foi o Google.
 */
@Component
@ConditionalOnProperty(name = "asus.oauth.google.enabled", havingValue = "true")
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final AuthService authService;
    private final String redirect;

    public OAuth2SuccessHandler(AuthService authService,
                                @Value("${asus.oauth.frontend-redirect:/login}") String redirect) {
        this.authService = authService;
        this.redirect = redirect;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        OAuth2User user = (OAuth2User) authentication.getPrincipal();
        String email = (String) user.getAttributes().get("email");
        String nome = (String) user.getAttributes().get("name");
        if (email == null || email.isBlank()) {
            getRedirectStrategy().sendRedirect(request, response, redirect + "#erro=sem_email");
            return;
        }
        AuthResponse tokens = authService.loginSocial(email, nome);
        String url = redirect + "#access_token=" + enc(tokens.accessToken())
                + "&refresh_token=" + enc(tokens.refreshToken());
        getRedirectStrategy().sendRedirect(request, response, url);
    }

    private static String enc(String v) {
        return URLEncoder.encode(v, StandardCharsets.UTF_8);
    }
}
