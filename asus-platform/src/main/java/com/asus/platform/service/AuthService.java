package com.asus.platform.service;

import com.asus.platform.domain.Usuario;
import com.asus.platform.repository.UsuarioRepository;
import com.asus.platform.security.JwtService;
import com.asus.platform.security.RateLimiter;
import com.asus.platform.web.TooManyRequestsException;
import com.asus.platform.web.UnauthorizedException;
import com.asus.platform.web.dto.AuthResponse;
import com.asus.platform.web.dto.UsuarioResponse;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Registro, login (com rate limit), refresh e perfil (Fase 7). */
@Service
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RateLimiter rateLimiter;
    private final AuditoriaService auditoriaService;

    public AuthService(UsuarioRepository usuarioRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService,
                       RateLimiter rateLimiter,
                       AuditoriaService auditoriaService) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.rateLimiter = rateLimiter;
        this.auditoriaService = auditoriaService;
    }

    @Transactional
    public AuthResponse registrar(String nome, String email, String senha) {
        if (usuarioRepository.findByEmail(email).isPresent()) {
            throw new IllegalArgumentException("E-mail ja cadastrado");
        }
        Usuario usuario = usuarioRepository.save(Usuario.builder()
                .nome(nome)
                .email(email)
                .senhaHash(passwordEncoder.encode(senha))
                .build());
        auditoriaService.registrar(null, usuario.getId(), "USUARIO_REGISTRADO",
                "Usuario", usuario.getId(), null, null, email);
        return tokensPara(usuario);
    }

    public AuthResponse login(String email, String senha) {
        if (!rateLimiter.permitido("login:" + email)) {
            throw new TooManyRequestsException("Muitas tentativas de login. Tente novamente em instantes.");
        }
        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new UnauthorizedException("Credenciais invalidas"));
        if (usuario.isAnonimizado()
                || usuario.getSenhaHash() == null
                || !passwordEncoder.matches(senha, usuario.getSenhaHash())) {
            throw new UnauthorizedException("Credenciais invalidas");
        }
        return tokensPara(usuario);
    }

    public AuthResponse refresh(String refreshToken) {
        try {
            Claims claims = jwtService.validar(refreshToken, "refresh");
            Usuario usuario = usuarioRepository.findById(Long.valueOf(claims.getSubject()))
                    .orElseThrow(() -> new UnauthorizedException("Usuario do token nao existe"));
            return tokensPara(usuario);
        } catch (JwtException | NumberFormatException e) {
            throw new UnauthorizedException("Refresh token invalido");
        }
    }

    public UsuarioResponse me(Long usuarioId) {
        return UsuarioResponse.de(usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new UnauthorizedException("Usuario nao encontrado")));
    }

    /**
     * Login social (Google/OAuth2): encontra o usuário pelo e-mail ou cria uma
     * conta só-OAuth (sem senha) e emite o par de tokens JWT.
     */
    @Transactional
    public AuthResponse loginSocial(String email, String nome) {
        Usuario usuario = usuarioRepository.findByEmail(email).orElseGet(() -> {
            Usuario novo = usuarioRepository.save(Usuario.builder()
                    .nome(nome == null || nome.isBlank() ? email : nome)
                    .email(email)
                    .build());
            auditoriaService.registrar(null, novo.getId(), "USUARIO_REGISTRADO_OAUTH",
                    "Usuario", novo.getId(), null, null, email);
            return novo;
        });
        if (usuario.isAnonimizado()) {
            throw new UnauthorizedException("Conta indisponivel");
        }
        return tokensPara(usuario);
    }

    private AuthResponse tokensPara(Usuario usuario) {
        String access = jwtService.gerarAccess(usuario.getId(), usuario.getEmail());
        String refresh = jwtService.gerarRefresh(usuario.getId(), usuario.getEmail());
        return new AuthResponse(access, refresh, "Bearer",
                jwtService.accessTtlSegundos(), UsuarioResponse.de(usuario));
    }
}
