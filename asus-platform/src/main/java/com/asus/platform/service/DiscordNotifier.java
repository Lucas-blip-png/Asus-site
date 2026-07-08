package com.asus.platform.service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Envia mensagens pro webhook do Discord da campanha (críticos, level-ups, sessões).
 * Best-effort: roda em background, falha só loga (nunca quebra a ação do jogador).
 */
@Component
public class DiscordNotifier {

    private static final Logger log = LoggerFactory.getLogger(DiscordNotifier.class);

    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(4))
            .build();

    /** Posta {@code conteudo} no webhook (assíncrono). URL nula/vazia é ignorada. */
    public void enviar(String webhookUrl, String conteudo) {
        if (webhookUrl == null || webhookUrl.isBlank() || conteudo == null || conteudo.isBlank()) {
            return;
        }
        // Defesa extra contra SSRF (a validação principal está no salvar da campanha).
        if (!webhookUrl.startsWith("https://discord.com/api/webhooks/")
                && !webhookUrl.startsWith("https://discordapp.com/api/webhooks/")) {
            return;
        }
        String json = "{\"content\":" + jsonString(conteudo) + "}";
        HttpRequest req = HttpRequest.newBuilder(URI.create(webhookUrl))
                .timeout(Duration.ofSeconds(6))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();
        CompletableFuture
                .supplyAsync(() -> {
                    try {
                        return http.send(req, HttpResponse.BodyHandlers.discarding()).statusCode();
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                })
                .whenComplete((status, err) -> {
                    if (err != null) {
                        log.warn("Discord webhook falhou: {}", err.getMessage());
                    } else if (status >= 300) {
                        log.warn("Discord webhook retornou HTTP {}", status);
                    }
                });
    }

    private static String jsonString(String s) {
        StringBuilder sb = new StringBuilder("\"");
        for (char c : s.toCharArray()) {
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> {
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
                }
            }
        }
        return sb.append('"').toString();
    }
}
