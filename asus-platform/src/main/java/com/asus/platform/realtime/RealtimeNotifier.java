package com.asus.platform.realtime;

import com.asus.platform.web.dto.RolagemResponse;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

/**
 * Publica eventos em tempo real nos topicos STOMP (plano, Fase 6).
 *
 * <p>Centraliza o uso do {@link SimpMessagingTemplate} para que os services
 * apenas chamem metodos tipados. Se o WebSocket nao estiver ativo, basta trocar
 * esta peca por um no-op.</p>
 */
@Component
public class RealtimeNotifier {

    private final SimpMessagingTemplate messaging;

    public RealtimeNotifier(SimpMessagingTemplate messaging) {
        this.messaging = messaging;
    }

    /** Nova rolagem (ou revelacao) de uma campanha. */
    public void rolagem(Long campanhaId, RolagemResponse rolagem) {
        messaging.convertAndSend("/topic/campanhas/" + campanhaId + "/rolagens", rolagem);
    }

    /** Mudanca de status (PV/PM/PE) de um personagem. */
    public void statusPersonagem(Long personagemId, Object status) {
        messaging.convertAndSend("/topic/personagens/" + personagemId + "/status", status);
    }

    /** Presenca de jogadores em uma campanha. */
    public void presenca(Long campanhaId, Object payload) {
        messaging.convertAndSend("/topic/campanhas/" + campanhaId + "/presenca", payload);
    }
}
