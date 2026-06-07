package com.asus.platform.security;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

/**
 * Rate limit simples em memoria (janela deslizante) para o login (plano, Seção 20).
 * Em producao com multiplas instancias, trocar por Redis/bucket distribuido.
 */
@Component
public class RateLimiter {

    private static final int MAX_TENTATIVAS = 5;
    private static final long JANELA_MS = 60_000L;

    private final Map<String, Deque<Long>> historico = new ConcurrentHashMap<>();

    /** true se a acao pode prosseguir; false se excedeu o limite na janela. */
    public boolean permitido(String chave) {
        long agora = System.currentTimeMillis();
        Deque<Long> tentativas = historico.computeIfAbsent(chave, k -> new ArrayDeque<>());
        synchronized (tentativas) {
            while (!tentativas.isEmpty() && agora - tentativas.peekFirst() > JANELA_MS) {
                tentativas.pollFirst();
            }
            if (tentativas.size() >= MAX_TENTATIVAS) {
                return false;
            }
            tentativas.addLast(agora);
            return true;
        }
    }
}
