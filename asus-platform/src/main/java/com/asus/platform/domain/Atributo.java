package com.asus.platform.domain;

/**
 * Os sete atributos do sistema ASUS (fonte: "Sistema de Atributos" e
 * "Asus - Projeto de Sistema").
 *
 * <ul>
 *   <li>FORCA — potência física, dano corpo a corpo, capacidade de carga (2x).</li>
 *   <li>CONSTITUICAO — vida e energia (+2 de cada por ponto); energia gasta por turno.</li>
 *   <li>DESTREZA (Proficiência) — habilidade manual; limite de Habilidades (metade).</li>
 *   <li>AGILIDADE — reflexos e Deslocamento (4 + 1 a cada 5 pontos).</li>
 *   <li>INTELIGENCIA — feitiçaria; limite de Feitiços (metade).</li>
 *   <li>SABEDORIA — instinto/percepção; limite de Bênçãos (metade).</li>
 *   <li>CARISMA — interação social e primeira impressão.</li>
 * </ul>
 */
public enum Atributo {
    FORCA("For"),
    CONSTITUICAO("Con"),
    DESTREZA("Des"),
    AGILIDADE("Agi"),
    INTELIGENCIA("Int"),
    SABEDORIA("Sab"),
    CARISMA("Car");

    private final String sigla;

    Atributo(String sigla) {
        this.sigla = sigla;
    }

    public String getSigla() {
        return sigla;
    }
}
