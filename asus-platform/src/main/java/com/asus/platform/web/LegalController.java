package com.asus.platform.web;

import com.asus.platform.web.dto.LegalDocumentoResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Documentos legais (plano, Seção 19.3 / Fase 13). Texto base, versionavel. */
@RestController
@RequestMapping("/api/legal")
public class LegalController {

    private static final String VERSAO = "1.0";

    @GetMapping("/termos")
    public LegalDocumentoResponse termos() {
        return new LegalDocumentoResponse("TERMOS", VERSAO, """
                # Termos de Uso — ASUS RPG Platform

                Ao usar a plataforma voce concorda em utiliza-la para fins de jogo de RPG,
                respeitando os direitos de outros usuarios e a legislacao aplicavel.
                Este e um texto base; substitua pelo termo juridico definitivo antes de produzir.
                """);
    }

    @GetMapping("/privacidade")
    public LegalDocumentoResponse privacidade() {
        return new LegalDocumentoResponse("PRIVACIDADE", VERSAO, """
                # Politica de Privacidade — ASUS RPG Platform

                Tratamos seus dados conforme a LGPD. Voce pode exportar seus dados em
                /api/me/export-data e solicitar a exclusao/anonimizacao em /api/me/delete-account.
                Este e um texto base; substitua pela politica juridica definitiva antes de produzir.
                """);
    }
}
