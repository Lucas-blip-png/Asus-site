package com.asus.platform.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Geracao e validacao de JWT (Fase 7).
 *
 * <p>Dois tipos de token: {@code access} (curto) e {@code refresh} (longo).
 * Segredo e TTLs vem de configuracao (env em producao).</p>
 */
@Service
public class JwtService {

    private final SecretKey key;
    private final long accessTtlMs;
    private final long refreshTtlMs;

    public JwtService(
            @Value("${asus.jwt.secret:troque-este-segredo-em-producao-asus-rpg-platform-0123456789}") String secret,
            @Value("${asus.jwt.access-ttl-min:15}") long accessTtlMin,
            @Value("${asus.jwt.refresh-ttl-days:7}") long refreshTtlDays) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessTtlMs = accessTtlMin * 60_000L;
        this.refreshTtlMs = refreshTtlDays * 24L * 60L * 60L * 1000L;
    }

    public String gerarAccess(Long usuarioId, String email) {
        return gerar(usuarioId, email, "access", accessTtlMs);
    }

    public String gerarRefresh(Long usuarioId, String email) {
        return gerar(usuarioId, email, "refresh", refreshTtlMs);
    }

    /** Token para confirmar o e-mail (link enviado por e-mail). Vale 3 dias. */
    public String gerarVerificacaoEmail(Long usuarioId, String email) {
        return gerar(usuarioId, email, "verificacao", 3L * 24L * 60L * 60L * 1000L);
    }

    /** Token para redefinir a senha ("esqueci minha senha"). Vale 1 hora. */
    public String gerarResetSenha(Long usuarioId, String email) {
        return gerar(usuarioId, email, "reset-senha", 60L * 60L * 1000L);
    }

    public long accessTtlSegundos() {
        return accessTtlMs / 1000L;
    }

    /** Valida assinatura/expiracao e confere o tipo; lanca {@link JwtException} se invalido. */
    public Claims validar(String token, String tipoEsperado) {
        Claims claims = Jwts.parser().verifyWith(key).build()
                .parseSignedClaims(token).getPayload();
        if (!tipoEsperado.equals(claims.get("tipo", String.class))) {
            throw new JwtException("Tipo de token invalido");
        }
        return claims;
    }

    private String gerar(Long usuarioId, String email, String tipo, long ttlMs) {
        Instant agora = Instant.now();
        return Jwts.builder()
                .subject(String.valueOf(usuarioId))
                .claim("email", email)
                .claim("tipo", tipo)
                .issuedAt(Date.from(agora))
                .expiration(Date.from(agora.plusMillis(ttlMs)))
                .signWith(key)
                .compact();
    }
}
