package com.asus.platform.web;

import com.asus.platform.security.UsuarioPrincipal;
import com.asus.platform.service.AuthService;
import com.asus.platform.web.dto.AuthResponse;
import com.asus.platform.web.dto.LoginRequest;
import com.asus.platform.web.dto.RefreshRequest;
import com.asus.platform.web.dto.RegisterRequest;
import com.asus.platform.web.dto.UsuarioResponse;
import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/** Autenticacao: registro, login, refresh e perfil (plano, Fase 7). */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService service;

    @Value("${asus.oauth.google.enabled:false}")
    private boolean googleOAuth;

    public AuthController(AuthService service) {
        this.service = service;
    }

    /** Config pública pro frontend (ex.: mostrar ou não o botão "Entrar com Google"). */
    @GetMapping("/config")
    public Map<String, Object> config() {
        return Map.of("googleOAuth", googleOAuth);
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse register(@Valid @RequestBody RegisterRequest req) {
        return service.registrar(req.nome(), req.email(), req.senha());
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest req) {
        return service.login(req.email(), req.senha());
    }

    @PostMapping("/refresh")
    public AuthResponse refresh(@Valid @RequestBody RefreshRequest req) {
        return service.refresh(req.refreshToken());
    }

    @GetMapping("/me")
    public UsuarioResponse me(@AuthenticationPrincipal UsuarioPrincipal principal) {
        return service.me(principal.id());
    }
}
