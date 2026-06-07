package com.asus.platform;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Ponto de entrada da ASUS RPG Platform.
 *
 * <p>Fase 1 — Nucleo SaaS minimo (ver plano de implementacao v4, secao 22).
 * O ASUS e o sistema oficial e padrao da plataforma.</p>
 */
@SpringBootApplication
public class AsusPlatformApplication {

    public static void main(String[] args) {
        SpringApplication.run(AsusPlatformApplication.class, args);
    }
}
