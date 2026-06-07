package com.asus.platform.realtime;

import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;

/** Recebe avisos de presenca dos clientes e os retransmite para a campanha (Fase 6). */
@Controller
public class PresencaController {

    private final RealtimeNotifier notifier;

    public PresencaController(RealtimeNotifier notifier) {
        this.notifier = notifier;
    }

    @MessageMapping("/campanhas/{id}/presenca")
    public void presenca(@DestinationVariable Long id, PresencaMensagem mensagem) {
        notifier.presenca(id, mensagem);
    }
}
