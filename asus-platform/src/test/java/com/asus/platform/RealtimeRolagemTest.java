package com.asus.platform;

import static org.assertj.core.api.Assertions.assertThat;

import com.asus.platform.web.dto.RolagemResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.lang.reflect.Type;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;

/** Prova que uma rolagem feita via REST chega em tempo real pelo WebSocket (Fase 6). */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class RealtimeRolagemTest {

    @LocalServerPort
    int port;

    @Autowired
    TestRestTemplate rest;

    @Autowired
    ObjectMapper objectMapper;

    @Test
    void rolagemChegaPeloWebSocket() throws Exception {
        long orgId = postId("/api/organizacoes",
                "{\"nome\":\"Org WS\",\"slug\":\"ws-test\"}");
        long campId = postId("/api/organizacoes/" + orgId + "/campanhas",
                "{\"nome\":\"Camp WS\",\"mestreId\":1}");

        WebSocketStompClient stomp = new WebSocketStompClient(new StandardWebSocketClient());
        // Usa o ObjectMapper do Spring (com suporte a LocalDateTime / JSR-310).
        MappingJackson2MessageConverter converter = new MappingJackson2MessageConverter();
        converter.setObjectMapper(objectMapper);
        stomp.setMessageConverter(converter);

        StompSession session = stomp
                .connectAsync("ws://localhost:" + port + "/ws", new StompSessionHandlerAdapter() {})
                .get(10, TimeUnit.SECONDS);

        BlockingQueue<RolagemResponse> recebidas = new LinkedBlockingQueue<>();
        session.subscribe("/topic/campanhas/" + campId + "/rolagens", new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return RolagemResponse.class;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                recebidas.add((RolagemResponse) payload);
            }
        });

        // Garante que o SUBSCRIBE chegou ao servidor antes de publicar.
        Thread.sleep(800);

        post("/api/campanhas/" + campId + "/rolagens", "{\"expressao\":\"1d20+5\",\"rotulo\":\"Ataque\"}");

        RolagemResponse recebida = recebidas.poll(8, TimeUnit.SECONDS);
        assertThat(recebida).isNotNull();
        assertThat(recebida.campanhaId()).isEqualTo(campId);
        assertThat(recebida.expressao()).isEqualTo("1d20+5");
        assertThat(recebida.total()).isNotNull(); // rolagem aberta -> resultado visivel

        session.disconnect();
        stomp.stop();
    }

    private long postId(String url, String json) throws Exception {
        return objectMapper.readTree(post(url, json)).get("id").asLong();
    }

    private String post(String url, String json) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return rest.postForEntity(url, new HttpEntity<>(json, headers), String.class).getBody();
    }
}
