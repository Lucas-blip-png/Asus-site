package com.asus.platform.realtime;

import java.util.concurrent.atomic.AtomicInteger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

/** Contador simples de sessoes WebSocket ativas (presenca global), Fase 6. */
@Component
public class PresenceListener {

    private static final Logger log = LoggerFactory.getLogger(PresenceListener.class);
    private final AtomicInteger online = new AtomicInteger();

    public int online() {
        return online.get();
    }

    @EventListener
    public void onConnect(SessionConnectedEvent event) {
        log.info("WebSocket conectado. Sessoes online={}", online.incrementAndGet());
    }

    @EventListener
    public void onDisconnect(SessionDisconnectEvent event) {
        log.info("WebSocket desconectado. Sessoes online={}", Math.max(0, online.decrementAndGet()));
    }
}
