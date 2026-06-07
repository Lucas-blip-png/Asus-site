package com.asus.platform.engine;

/**
 * Motor de regras de um sistema (plano, secao 5.1).
 *
 * <p>O ASUS e o sistema principal, mas a plataforma e estruturada como motor
 * generico para permitir versoes (ASUS_V1, ASUS_V2...) e sistemas futuros.</p>
 */
public interface GameSystemEngine {

    String getSystemId();

    String getVersion();

    ResultadoCalculo calcular(ContextoCalculo contexto);
}
