package com.asus.platform.service;

import com.asus.platform.domain.Personagem;
import com.asus.platform.domain.PersonagemSnapshot;
import com.asus.platform.repository.PersonagemSnapshotRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.springframework.stereotype.Service;

/** Cria e lista snapshots da ficha (plano, secao 11 / criterio de aceite 8). */
@Service
public class SnapshotService {

    private final PersonagemSnapshotRepository repository;
    private final ObjectMapper objectMapper;

    public SnapshotService(PersonagemSnapshotRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    public PersonagemSnapshot criar(Personagem personagem, String motivo) {
        String json;
        try {
            json = objectMapper.writeValueAsString(personagem);
        } catch (JsonProcessingException e) {
            json = "{}";
        }
        PersonagemSnapshot snapshot = PersonagemSnapshot.builder()
                .personagemId(personagem.getId())
                .motivo(motivo)
                .jsonFicha(json)
                .build();
        return repository.save(snapshot);
    }

    public List<PersonagemSnapshot> listar(Long personagemId) {
        return repository.findByPersonagemIdOrderByCriadoEmDesc(personagemId);
    }
}
