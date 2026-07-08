package com.asus.platform.web;

import com.asus.platform.domain.MensagemCampanha;
import com.asus.platform.repository.MensagemCampanhaRepository;
import com.asus.platform.repository.UsuarioRepository;
import com.asus.platform.security.UsuarioPrincipal;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/** Chat de texto da campanha (mensagens simples, junto das rolagens no painel). */
@RestController
@RequestMapping("/api/campanhas/{id}/mensagens")
public class MensagemCampanhaController {

    private final MensagemCampanhaRepository repository;
    private final UsuarioRepository usuarioRepository;
    private final SimpMessagingTemplate messaging;

    public MensagemCampanhaController(MensagemCampanhaRepository repository,
                                      UsuarioRepository usuarioRepository,
                                      SimpMessagingTemplate messaging) {
        this.repository = repository;
        this.usuarioRepository = usuarioRepository;
        this.messaging = messaging;
    }

    @GetMapping
    public List<MensagemCampanha> listar(@PathVariable Long id) {
        return repository.findTop100ByCampanhaIdOrderByCriadoEmDescIdDesc(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public MensagemCampanha enviar(@PathVariable Long id,
                                   @RequestBody Map<String, String> body,
                                   @AuthenticationPrincipal UsuarioPrincipal principal) {
        String texto = body.get("texto") == null ? "" : body.get("texto").trim();
        if (texto.isEmpty()) {
            throw new IllegalArgumentException("mensagem vazia");
        }
        if (texto.length() > 2000) {
            texto = texto.substring(0, 2000);
        }
        Long usuarioId = principal != null ? principal.id() : null;
        String autor = usuarioId == null ? "Anônimo"
                : usuarioRepository.findById(usuarioId)
                        .map(com.asus.platform.domain.Usuario::getNome).orElse("Jogador");
        MensagemCampanha m = repository.save(MensagemCampanha.builder()
                .campanhaId(id).usuarioId(usuarioId).autor(autor).texto(texto).build());
        messaging.convertAndSend("/topic/campanhas/" + id + "/mensagens", m);
        return m;
    }
}
