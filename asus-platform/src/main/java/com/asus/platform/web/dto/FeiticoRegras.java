package com.asus.platform.web.dto;

import java.util.List;

/** Regras de construcao de feiticos (livro "Criacao de Feiticos"). */
public record FeiticoRegras(
        List<Linha> circulos,
        List<Linha> alcance,
        List<Linha> poderDano,
        List<Linha> poderCura,
        List<Linha> duracao,
        List<Linha> modificadores) {

    public record Linha(String nome, String efeito, String custoPm) {}

    public static FeiticoRegras padrao() {
        return new FeiticoRegras(
                List.of(
                        new Linha("1 Circulo", "1d", "1 PM"),
                        new Linha("2 Circulo", "3d", "3 PM"),
                        new Linha("3 Circulo", "5d", "6 PM"),
                        new Linha("4 Circulo", "8d", "10 PM")),
                List.of(
                        new Linha("Toque / Pessoal", "-", "0 PM"),
                        new Linha("Curto / Raio 3m / 3 seres", "-", "2 PM"),
                        new Linha("Medio / Raio 6m", "-", "5 PM"),
                        new Linha("Longo / Raio 10m", "-", "10 PM"),
                        new Linha("Extremo / Raio 15m+", "-", "25 PM")),
                List.of(
                        new Linha("Nivel 1", "1d6", "1 PM"),
                        new Linha("Nivel 2", "2d6", "4 PM"),
                        new Linha("Nivel 3", "4d6", "8 PM"),
                        new Linha("Nivel 4", "6d6", "12 PM"),
                        new Linha("Nivel 5", "10d6", "20 PM")),
                List.of(
                        new Linha("Nivel 1", "1d8", "1 PM"),
                        new Linha("Nivel 2", "2d8", "4 PM"),
                        new Linha("Nivel 3", "4d8", "8 PM"),
                        new Linha("Nivel 4", "6d8", "12 PM"),
                        new Linha("Nivel 5", "10d8", "20 PM")),
                List.of(
                        new Linha("1 turno / Instantaneo", "-", "0 PM"),
                        new Linha("3 turnos", "-", "3 PM"),
                        new Linha("5 turnos", "-", "6 PM"),
                        new Linha("Cena", "-", "12 PM"),
                        new Linha("Sustentado", "1 PM por turno", "1 PM")),
                List.of(
                        new Linha("Penetrante", "Ignora 5 de armadura por circulo", "3 PM/circulo"),
                        new Linha("Encadeado", "Salta entre inimigos", "2 PM a cada 2 seres"),
                        new Linha("Rapido", "Diminui 1 passo na execucao", "10 PM"),
                        new Linha("Silencioso", "Conjura sem gestos/palavras", "3 PM"),
                        new Linha("Ritual", "Demora 1 rodada", "-6 PM"),
                        new Linha("Sobrecarga de Grau", "Lanca 1 circulo acima do dominado", "Custo do circulo x3")));
    }
}
